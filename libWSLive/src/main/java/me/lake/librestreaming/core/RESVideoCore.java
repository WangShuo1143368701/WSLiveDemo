package me.lake.librestreaming.core;

import android.graphics.SurfaceTexture;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.core.listener.RESVideoChangeListener;
import me.lake.librestreaming.encoder.MediaVideoEncoder;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;


public interface RESVideoCore {
    int OVERWATCH_TEXTURE_ID = 10;
    boolean prepare(RESConfig resConfig);

    void updateCamTexture(SurfaceTexture camTex);

    void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void stopPreview(boolean releaseTexture);

    boolean startStreaming(RESFlvDataCollecter flvDataCollecter);

    boolean stopStreaming();

    boolean destroy();

    void reSetVideoBitrate(int bitrate);

    int getVideoBitrate();

    void reSetVideoFPS(int fps);

    void reSetVideoSize(RESCoreParameters newParameters);

    void setCurrentCamera(int cameraIndex);

    void takeScreenShot(RESScreenShotListener listener);

    void setVideoChangeListener(RESVideoChangeListener listener);

    float getDrawFrameRate();

    void setVideoEncoder(final MediaVideoEncoder encoder);

    void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror);
}
