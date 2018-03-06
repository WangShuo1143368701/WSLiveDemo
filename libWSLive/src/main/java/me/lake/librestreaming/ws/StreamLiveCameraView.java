package me.lake.librestreaming.ws;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.lake.librestreaming.client.RESClient;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.core.listener.RESVideoChangeListener;
import me.lake.librestreaming.encoder.MediaAudioEncoder;
import me.lake.librestreaming.encoder.MediaEncoder;
import me.lake.librestreaming.encoder.MediaMuxerWrapper;
import me.lake.librestreaming.encoder.MediaVideoEncoder;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.tools.CameraUtil;
import me.lake.librestreaming.ws.filter.audiofilter.SetVolumeAudioFilter;

/**
 * Created by WangShuo on 2017/6/11.
 */

public class StreamLiveCameraView extends FrameLayout {

    private static final String TAG = "StreamLiveCameraView";

    private Context mContext;
    private AspectTextureView textureView;
    private final List<RESConnectionListener> outerStreamStateListeners = new ArrayList<>();

    private static RESClient resClient;
    private static RESConfig resConfig;
    private static int quality_value_min = 400 * 1024;
    private static int quality_value_max = 700 * 1024;

    public StreamLiveCameraView(Context context) {
        super(context);
        this.mContext=context;
    }

    public StreamLiveCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext=context;
    }

    public static synchronized RESClient getRESClient() {
        if (resClient == null) {
            resClient = new RESClient();
        }
        return resClient;
    }

    /**
     * 根据AVOption初始化&打开预览
     * @param avOption
     */
    public void init(Context context , StreamAVOption avOption) {
        if (avOption == null) {
            throw new IllegalArgumentException("AVOption is null.");
        }
        compatibleSize(avOption);
        resClient = getRESClient();
        setContext(mContext);
        resConfig = StreamConfig.build(context,avOption);
        boolean isSucceed = resClient.prepare(resConfig);
        if (!isSucceed) {
            Log.w(TAG, "推流prepare方法返回false, 状态异常.");
            return;
        }
        initPreviewTextureView();
        addListenerAndFilter();
    }

    private void compatibleSize(StreamAVOption avOptions) {
        Camera.Size cameraSize = CameraUtil.getInstance().getBestSize(CameraUtil.getFrontCameraSize(),Integer.parseInt("800"));
        if(!CameraUtil.hasSupportedFrontVideoSizes){
            if(null == cameraSize || cameraSize.width <= 0){
                avOptions.videoWidth = 720;
                avOptions.videoHeight = 480;
            }else{
                avOptions.videoWidth = cameraSize.width;
                avOptions.videoHeight = cameraSize.height;
            }
        }
    }

    private void initPreviewTextureView() {
        if (textureView == null && resClient != null) {
            textureView = new AspectTextureView(getContext());
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            this.removeAllViews();
            this.addView(textureView);
            textureView.setKeepScreenOn(true);
            textureView.setSurfaceTextureListener(surfaceTextureListenerImpl);
            Size s = resClient.getVideoSize();
            textureView.setAspectRatio(AspectTextureView.MODE_OUTSIDE, ((double) s.getWidth() / s.getHeight()));
        }
    }

    private void addListenerAndFilter() {
        if (resClient != null) {
            resClient.setConnectionListener(ConnectionListener);
            resClient.setVideoChangeListener(VideoChangeListener);
            resClient.setSoftAudioFilter(new SetVolumeAudioFilter());
        }
    }

    /**
     * 是否推流
     */
    public boolean isStreaming(){
        if(resClient != null){
           return resClient.isStreaming;
        }
        return false;
    }

    /**
     * 开始推流
     */
    public void startStreaming(String rtmpUrl){
        if(resClient != null){
            resClient.startStreaming(rtmpUrl);
        }
    }

    /**
     * 停止推流
     */
    public void stopStreaming(){
        if(resClient != null){
            resClient.stopStreaming();
        }
    }

    /**
     * 开始录制
     */
    private MediaMuxerWrapper mMuxer;
    private boolean isRecord = false;
    public void startRecord(){
        if(resClient != null){
            try {
                mMuxer = new MediaMuxerWrapper(".mp4");    // if you record audio only, ".m4a" is also OK.
                new MediaVideoEncoder(mMuxer, mMediaEncoderListener, StreamAVOption.recordVideoWidth, StreamAVOption.recordVideoHeight);
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);

                mMuxer.prepare();
                mMuxer.startRecording();
                isRecord = true;
            } catch (IOException e) {
                isRecord = false;
                e.printStackTrace();
            }
        }
    }
    /**
     * 停止录制
     */
    public String stopRecord() {
        if (mMuxer != null) {
            String path = mMuxer.getFilePath();
            mMuxer.stopRecording();
            mMuxer = null;
            System.gc();
            return path;
        }
        System.gc();
        isRecord = false;
        return null;
    }

    /**
     * 是否在录制
     */
    public boolean isRecord() {
        return isRecord;
    }

    /**
     * 切换摄像头
     */
    public void swapCamera(){
        if(resClient != null){
            resClient.swapCamera();
        }
    }

    /**
     * 摄像头焦距 [0.0f,1.0f]
     */
    public void setZoomByPercent(float targetPercent){
        if(resClient != null){
            resClient.setZoomByPercent(targetPercent);
        }
    }

    /**
     *摄像头开关闪光灯
     */
    public void toggleFlashLight(){
        if(resClient != null){
            resClient.toggleFlashLight();
        }
    }

    /**
     * 推流过程中，重新设置帧率
     */
    public void reSetVideoFPS(int fps){
        if(resClient != null){
            resClient.reSetVideoFPS(fps);
        }
    }

    /**
     * 推流过程中，重新设置码率
     */
    public void reSetVideoBitrate(int bitrate){
        if(resClient != null){
            resClient.reSetVideoBitrate(bitrate);
        }
    }

    /**
     * 截图
     */
    public void takeScreenShot(RESScreenShotListener listener){
        if(resClient != null){
            resClient.takeScreenShot(listener);
        }
    }

    /**
     * 镜像
     * @param isEnableMirror   是否启用镜像功能 总开关
     * @param isEnablePreviewMirror  是否开启预览镜像
     * @param isEnableStreamMirror   是否开启推流镜像
     */
    public void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror) {
        if(resClient != null) {
            resClient.setMirror(isEnableMirror, isEnablePreviewMirror, isEnableStreamMirror);
        }
    }


    /**
     * 设置滤镜
     */
    public void setHardVideoFilter(BaseHardVideoFilter baseHardVideoFilter){
        if(resClient != null){
            resClient.setHardVideoFilter(baseHardVideoFilter);
        }
    }

    /**
     * 获取BufferFreePercent
     */
    public float getSendBufferFreePercent() {
        return resClient.getSendBufferFreePercent();
    }

    /**
     * AVSpeed 推流速度 和网络相关
     */
    public int getAVSpeed() {
        return resClient.getAVSpeed();
    }

    /**
     * 设置上下文
     */
    public void setContext(Context context){
        if(resClient != null){
            resClient.setContext(context);
        }
    }

    /**
     * destroy
     */
    public void destroy(){
        if (resClient != null) {
            resClient.setConnectionListener(null);
            resClient.setVideoChangeListener(null);
            if(resClient.isStreaming){
                resClient.stopStreaming();
            }
            if(isRecord()){
                stopRecord();
            }
            resClient.destroy();
        }
    }

    /**
     * 添加推流状态监听
     * @param listener
     */
    public void addStreamStateListener(RESConnectionListener listener) {
        if (listener != null && !outerStreamStateListeners.contains(listener)) {
            outerStreamStateListeners.add(listener);
        }
    }

    RESConnectionListener ConnectionListener =new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int result) {
            if(result == 1){
               resClient.stopStreaming();
            }

            for (RESConnectionListener listener: outerStreamStateListeners) {
                listener.onOpenConnectionResult(result);
            }
        }

        @Override
        public void onWriteError(int errno) {

            for (RESConnectionListener listener: outerStreamStateListeners) {
                listener.onWriteError(errno);
            }
        }

        @Override
        public void onCloseConnectionResult(int result) {

            for (RESConnectionListener listener: outerStreamStateListeners) {
                listener.onCloseConnectionResult(result);
            }
        }
    };

    RESVideoChangeListener VideoChangeListener = new RESVideoChangeListener() {
        @Override
        public void onVideoSizeChanged(int width, int height) {
            if(textureView != null) {
                textureView.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) width) / height);
            }
        }
    };

    TextureView.SurfaceTextureListener surfaceTextureListenerImpl  = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (resClient != null) {
                resClient.startPreview(surface, width, height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if (resClient != null) {
                resClient.updatePreview(width, height);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (resClient != null) {
                resClient.stopPreview(true);
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder && resClient != null)
                resClient.setVideoEncoder((MediaVideoEncoder) encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder && resClient != null)
                resClient.setVideoEncoder(null);
        }
    };
}
