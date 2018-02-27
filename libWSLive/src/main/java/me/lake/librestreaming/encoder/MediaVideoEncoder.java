package me.lake.librestreaming.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

import me.lake.librestreaming.encoder.utils.RenderHandler;


public class MediaVideoEncoder extends MediaEncoder {
    private static final boolean DEBUG = false;    // TODO set false on release
    private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";
    // parameters for recording
    private static final int FRAME_RATE = 24;
    private static final float BPP = 0.25f;

    private final int mWidth;
    private final int mHeight;
    private RenderHandler mRenderHandler;
    private Surface mSurface;

    private int previewW, previewH;         //预览宽高
    private float[] mvpMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    private boolean isMatrixCalc = false;

    public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int width, final int height) {
        super(muxer, listener);
        if (DEBUG) Log.i(TAG, "MediaVideoEncoder: ");
        mWidth = width;
        mHeight = height;
        mRenderHandler = RenderHandler.createHandler(TAG);
    }

    public boolean frameAvailableSoon(final float[] tex_matrix) {
        boolean result;
        if (result = super.frameAvailableSoon())
            mRenderHandler.draw(tex_matrix);
        return result;
    }

    public boolean frameAvailableSoon(final float[] tex_matrix, final float[] mvp_matrix) {
        boolean result;
        if (result = super.frameAvailableSoon())
            mRenderHandler.draw(tex_matrix, mvp_matrix);
        return result;
    }

    @Override
    public boolean frameAvailableSoon() {
        boolean result;
        if (result = super.frameAvailableSoon())
            mRenderHandler.draw(null);
        return result;
    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) Log.i(TAG, "prepare: ");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 6);
        if (DEBUG) Log.i(TAG, "format: " + format);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();    // API >= 18
        mMediaCodec.start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    public void setEglContext(final EGLContext shared_context, final int tex_id) {
        mRenderHandler.setEglContext(shared_context, tex_id, mSurface, true);
    }

    @Override
    protected void release() {
        if (DEBUG) Log.i(TAG, "release:");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        super.release();
    }

    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        if (DEBUG) Log.i(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void signalEndOfInputStream() {
        if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        mMediaCodec.signalEndOfInputStream();    // API >= 18
        mIsEOS = true;
    }

    public void setPreviewWH(int previewW, int previewH) {
        this.previewW = previewW;
        this.previewH = previewH;
    }

    public float[] getMvpMatrix() {
        if (previewW < 1 || previewH < 1) return null;
        if (isMatrixCalc) return mvpMatrix;

        float encodeWHRatio = mWidth * 1.0f / mHeight;
        float previewWHRatio = previewW * 1.0f / previewH;

        float[] projection = new float[16];
        float[] camera = new float[16];

        if (encodeWHRatio > previewWHRatio) {
            Matrix.orthoM(projection, 0, -1, 1, -previewWHRatio / encodeWHRatio, previewWHRatio / encodeWHRatio, 1, 3);
        } else {
            Matrix.orthoM(projection, 0, -encodeWHRatio / previewWHRatio, encodeWHRatio / previewWHRatio, -1, 1, 1, 3);
        }
        Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projection, 0, camera, 0);

        isMatrixCalc = true;

        return mvpMatrix;
    }

}
