package me.lake.librestreaming.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MediaMuxerWrapper {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "MediaMuxerWrapper";

	private static final String DIR_NAME = "WSLive";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

	private String mOutputPath;
	private final MediaMuxer mMediaMuxer;	// API >= 18
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private MediaEncoder mVideoEncoder, mAudioEncoder;

	/**
	 * Constructor
	 * @param ext extension of output file
	 * @throws IOException
	 */
	public MediaMuxerWrapper(String ext) throws IOException {
		if (TextUtils.isEmpty(ext)) ext = ".mp4";
		try {
			mOutputPath = getCaptureFile(Environment.DIRECTORY_MOVIES, ext).toString();
			//mOutputPath =newTmpDir("Movies");/* getCaptureFile(Environment.DIRECTORY_MOVIES, ext).toString();*/
		} catch (final NullPointerException e) {
			throw new RuntimeException("This app has no permission of writing external storage");
		}
		mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		mEncoderCount = mStatredCount = 0;
		mIsStarted = false;
	}

	public static final String ROOT_DIR = "video";
	private static final String DIR_TMP = "tmp";
	private static Context mContext;

	public static void setContext(Context context){
		mContext = context;
	}
	/**
	 * 新建tmp目录,tmp/xxx/
	 *
	 * @param dirName
	 * @return
	 */
	public static String newTmpDir(String dirName) {
		File tmpDir = new File(getStorageRoot(mContext, ROOT_DIR, true), DIR_TMP);
		if (!tmpDir.exists() || !tmpDir.isDirectory()) {
			tmpDir.mkdirs();
		}
		File dir = new File(tmpDir, dirName);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdirs();
		}
		return dir.getAbsolutePath()+getDateTimeString() + ".mp4";
	}

	/**
	 * 获取缓存root路径
	 *
	 * @param context
	 * @param isExternFirst 是否外存优先
	 * @return
	 */
	public static File getStorageRoot(Context context, String dirName, boolean isExternFirst) {
		File cacheDir = null;
		if ((Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) && isExternFirst) {
			cacheDir = context.getExternalCacheDir();
		} else {
			cacheDir = context.getCacheDir();
		}
		File dir = new File(cacheDir, dirName);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdirs();
		}
		return dir;
	}

	public String getOutputPath() {
		return mOutputPath;
	}

	public void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

	public void stopRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.stopRecording();
		mVideoEncoder = null;
		if (mAudioEncoder != null)
			mAudioEncoder.stopRecording();
		mAudioEncoder = null;
	}

	public synchronized boolean isStarted() {
		return mIsStarted;
	}

//**********************************************************************
//**********************************************************************
	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
	 */
	/*package*/ void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoder) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		} else if (encoder instanceof MediaAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/ synchronized boolean start() {
		if (DEBUG) Log.v(TAG,  "start:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	*/
	/*package*/ synchronized void stop() {
		if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			mMediaMuxer.stop();
			mMediaMuxer.release();
			mIsStarted = false;
			if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ synchronized int addTrack(final MediaFormat format) {
		if (mIsStarted)
			throw new IllegalStateException("muxer already started");
		final int trackIx = mMediaMuxer.addTrack(format);
		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
		return trackIx;
	}

	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/ synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mStatredCount > 0)
			mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
	}

//**********************************************************************
//**********************************************************************
    /**
     * generate output file
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static final File getCaptureFile(final String type, final String ext) {
		final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
		Log.d(TAG, "path=" + dir.toString());
		dir.mkdirs();
        if (dir.canWrite()) {
        	return new File(dir, getDateTimeString() + ext);
        }
    	return null;
    }

    /**
     * get current date and time as String
     * @return
     */
    private static final String getDateTimeString() {
    	final GregorianCalendar now = new GregorianCalendar();
    	return mDateTimeFormat.format(now.getTime());
    }

    public String getFilePath(){
		return mOutputPath;
	}

}
