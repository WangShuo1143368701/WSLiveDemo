package me.lake.librestreaming.client;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;
import java.util.List;

import me.lake.librestreaming.core.CameraHelper;
import me.lake.librestreaming.core.RESHardVideoCore;
import me.lake.librestreaming.core.RESSoftVideoCore;
import me.lake.librestreaming.core.RESVideoCore;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.core.listener.RESVideoChangeListener;
import me.lake.librestreaming.encoder.MediaVideoEncoder;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.tools.BuffSizeCalculator;
import me.lake.librestreaming.tools.LogTools;


public class RESVideoClient {
    RESCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private Camera camera;
    public SurfaceTexture camTexture;
    private int cameraNum;
    private int currentCameraIndex;
    private RESVideoCore videoCore;
    private boolean isStreaming;
    private boolean isPreviewing;

    public RESVideoClient(RESCoreParameters parameters) {
        resCoreParameters = parameters;
        cameraNum = Camera.getNumberOfCameras();
        currentCameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
        isStreaming = false;
        isPreviewing = false;
    }

    public boolean prepare(RESConfig resConfig) {
        synchronized (syncOp) {
            if ((cameraNum - 1) >= resConfig.getDefaultCamera()) {
                currentCameraIndex = resConfig.getDefaultCamera();
            }
            if (null == (camera = createCamera(currentCameraIndex))) {
                LogTools.e("can not open camera");
                return false;
            }
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.selectCameraPreviewWH(parameters, resCoreParameters, resConfig.getTargetPreviewSize());
            CameraHelper.selectCameraFpsRange(parameters, resCoreParameters);
            if (resConfig.getVideoFPS() > resCoreParameters.previewMaxFps / 1000) {
                resCoreParameters.videoFPS = resCoreParameters.previewMaxFps / 1000;
            } else {
                resCoreParameters.videoFPS = resConfig.getVideoFPS();
            }
            resoveResolution(resCoreParameters, resConfig.getTargetVideoSize());
            if (!CameraHelper.selectCameraColorFormat(parameters, resCoreParameters)) {
                LogTools.e("CameraHelper.selectCameraColorFormat,Failed");
                resCoreParameters.dump();
                return false;
            }
            if (!CameraHelper.configCamera(camera, resCoreParameters)) {
                LogTools.e("CameraHelper.configCamera,Failed");
                resCoreParameters.dump();
                return false;
            }
            switch (resCoreParameters.filterMode) {
                case RESCoreParameters.FILTER_MODE_SOFT:
                    videoCore = new RESSoftVideoCore(resCoreParameters);
                    break;
                case RESCoreParameters.FILTER_MODE_HARD:
                    videoCore = new RESHardVideoCore(resCoreParameters);
                    break;
            }
            if (!videoCore.prepare(resConfig)) {
                return false;
            }
            videoCore.setCurrentCamera(currentCameraIndex);
            prepareVideo();
            return true;
        }
    }

    private Camera createCamera(int cameraId) {
        try {
            camera = Camera.open(cameraId);
            camera.setDisplayOrientation(0);
        } catch (SecurityException e) {
            LogTools.trace("no permission", e);
            return null;
        } catch (Exception e) {
            LogTools.trace("camera.open()failed", e);
            return null;
        }
        return camera;
    }

    private boolean prepareVideo() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            camera.addCallbackBuffer(new byte[resCoreParameters.previewBufferSize]);
            camera.addCallbackBuffer(new byte[resCoreParameters.previewBufferSize]);
        }
        return true;
    }

    private boolean startVideo() {
        camTexture = new SurfaceTexture(RESVideoCore.OVERWATCH_TEXTURE_ID);
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    synchronized (syncOp) {
                        if (videoCore != null && data != null) {
                            ((RESSoftVideoCore) videoCore).queueVideo(data);
                        }
                        camera.addCallbackBuffer(data);
                    }
                }
            });
        } else {
            camTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    synchronized (syncOp) {
                        if (videoCore != null) {
                            ((RESHardVideoCore) videoCore).onFrameAvailable();
                        }
                    }
                }
            });
        }
        try {
            camera.setPreviewTexture(camTexture);
        } catch (IOException e) {
            LogTools.trace(e);
            camera.release();
            return false;
        }
        camera.startPreview();
        return true;
    }

    public boolean startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        synchronized (syncOp) {
            if (!isStreaming && !isPreviewing) {
                if (!startVideo()) {
                    resCoreParameters.dump();
                    LogTools.e("RESVideoClient,start(),failed");
                    return false;
                }
                videoCore.updateCamTexture(camTexture);
            }
            videoCore.startPreview(surfaceTexture, visualWidth, visualHeight);
            isPreviewing = true;
            return true;
        }
    }

    public void updatePreview(int visualWidth, int visualHeight) {
        videoCore.updatePreview(visualWidth, visualHeight);
    }

    public boolean stopPreview(boolean releaseTexture) {
        synchronized (syncOp) {
            if (isPreviewing) {
                videoCore.stopPreview(releaseTexture);
                if (!isStreaming) {
                    camera.stopPreview();
                    videoCore.updateCamTexture(null);
                    camTexture.release();
                }
            }
            isPreviewing = false;
            return true;
        }
    }

    public boolean startStreaming(RESFlvDataCollecter flvDataCollecter) {
        synchronized (syncOp) {
            if (!isStreaming && !isPreviewing) {
                if (!startVideo()) {
                    resCoreParameters.dump();
                    LogTools.e("RESVideoClient,start(),failed");
                    return false;
                }
                videoCore.updateCamTexture(camTexture);
            }
            videoCore.startStreaming(flvDataCollecter);
            isStreaming = true;
            return true;
        }
    }

    public boolean stopStreaming() {
        synchronized (syncOp) {
            if (isStreaming) {
                videoCore.stopStreaming();
                if (!isPreviewing) {
                    camera.stopPreview();
                    videoCore.updateCamTexture(null);
                    camTexture.release();
                }
            }
            isStreaming = false;
            return true;
        }
    }


    public boolean destroy() {
        synchronized (syncOp) {
            camera.release();
            videoCore.destroy();
            videoCore = null;
            camera = null;
            return true;
        }
    }

    public boolean swapCamera() {
        synchronized (syncOp) {
            LogTools.d("RESClient,swapCamera()");
            camera.stopPreview();
            camera.release();
            camera = null;
            if (null == (camera = createCamera(currentCameraIndex = (++currentCameraIndex) % cameraNum))) {
                LogTools.e("can not swap camera");
                return false;
            }
            videoCore.setCurrentCamera(currentCameraIndex);
            CameraHelper.selectCameraFpsRange(camera.getParameters(), resCoreParameters);
            if (!CameraHelper.configCamera(camera, resCoreParameters)) {
                camera.release();
                return false;
            }
            prepareVideo();
            camTexture.release();
            videoCore.updateCamTexture(null);
            startVideo();
            videoCore.updateCamTexture(camTexture);
            return true;
        }
    }

    public boolean toggleFlashLight() {
        synchronized (syncOp) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                List<String> flashModes = parameters.getSupportedFlashModes();
                String flashMode = parameters.getFlashMode();
                if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(parameters);
                        return true;
                    }
                } else if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                        return true;
                    }
                }
            } catch (Exception e) {
                LogTools.d("toggleFlashLight,failed" + e.getMessage());
                return false;
            }
            return false;
        }
    }

    public boolean setZoomByPercent(float targetPercent) {
        synchronized (syncOp) {
            targetPercent = Math.min(Math.max(0f, targetPercent), 1f);
            Camera.Parameters p = camera.getParameters();
            p.setZoom((int) (p.getMaxZoom() * targetPercent));
            camera.setParameters(p);
            return true;
        }
    }

    public void reSetVideoBitrate(int bitrate) {
        synchronized (syncOp) {
            if (videoCore != null) {
                videoCore.reSetVideoBitrate(bitrate);
            }
        }
    }

    public int getVideoBitrate() {
        synchronized (syncOp) {
            if (videoCore != null) {
                return videoCore.getVideoBitrate();
            } else {
                return 0;
            }
        }
    }

    public void reSetVideoFPS(int fps) {
        synchronized (syncOp) {
            int targetFps;
            if (fps > resCoreParameters.previewMaxFps / 1000) {
                targetFps = resCoreParameters.previewMaxFps / 1000;
            } else {
                targetFps = fps;
            }
            if (videoCore != null) {
                videoCore.reSetVideoFPS(targetFps);
            }
        }
    }

    public boolean reSetVideoSize(Size targetVideoSize) {
        synchronized (syncOp) {
            RESCoreParameters newParameters = new RESCoreParameters();
            newParameters.isPortrait = resCoreParameters.isPortrait;
            newParameters.filterMode = resCoreParameters.filterMode;
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.selectCameraPreviewWH(parameters, newParameters, targetVideoSize);
            resoveResolution(newParameters, targetVideoSize);
            boolean needRestartCamera = (newParameters.previewVideoHeight != resCoreParameters.previewVideoHeight
                    || newParameters.previewVideoWidth != resCoreParameters.previewVideoWidth);
            if (needRestartCamera) {
                newParameters.previewBufferSize = BuffSizeCalculator.calculator(resCoreParameters.previewVideoWidth,
                        resCoreParameters.previewVideoHeight, resCoreParameters.previewColorFormat);
                resCoreParameters.previewVideoWidth = newParameters.previewVideoWidth;
                resCoreParameters.previewVideoHeight = newParameters.previewVideoHeight;
                resCoreParameters.previewBufferSize  = newParameters.previewBufferSize;
                if ((isPreviewing || isStreaming)) {
                    LogTools.d("RESClient,reSetVideoSize.restartCamera");
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    if (null == (camera = createCamera(currentCameraIndex))) {
                        LogTools.e("can not createCamera camera");
                        return false;
                    }
                    if (!CameraHelper.configCamera(camera, resCoreParameters)) {
                        camera.release();
                        return false;
                    }
                    prepareVideo();
                    videoCore.updateCamTexture(null);
                    camTexture.release();
                    startVideo();
                    videoCore.updateCamTexture(camTexture);
                }
            }
            videoCore.reSetVideoSize(newParameters);
            return true;
        }
    }

    public BaseSoftVideoFilter acquireSoftVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            return ((RESSoftVideoCore) videoCore).acquireVideoFilter();
        }
        return null;
    }

    public void releaseSoftVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            ((RESSoftVideoCore) videoCore).releaseVideoFilter();
        }
    }

    public void setSoftVideoFilter(BaseSoftVideoFilter baseSoftVideoFilter) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            ((RESSoftVideoCore) videoCore).setVideoFilter(baseSoftVideoFilter);
        }
    }

    public BaseHardVideoFilter acquireHardVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_HARD) {
            return ((RESHardVideoCore) videoCore).acquireVideoFilter();
        }
        return null;
    }

    public void releaseHardVideoFilter() {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_HARD) {
            ((RESHardVideoCore) videoCore).releaseVideoFilter();
        }
    }

    public void setHardVideoFilter(BaseHardVideoFilter baseHardVideoFilter) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_HARD) {
            ((RESHardVideoCore) videoCore).setVideoFilter(baseHardVideoFilter);
        }
    }

    public void takeScreenShot(RESScreenShotListener listener) {
        synchronized (syncOp) {
            if (videoCore != null) {
                videoCore.takeScreenShot(listener);
            }
        }
    }

    public void setVideoChangeListener(RESVideoChangeListener listener) {
        synchronized (syncOp) {
            if (videoCore != null) {
                videoCore.setVideoChangeListener(listener);
            }
        }
    }

    public float getDrawFrameRate() {
        synchronized (syncOp) {
            return videoCore == null ? 0 : videoCore.getDrawFrameRate();
        }
    }

    private void resoveResolution(RESCoreParameters resCoreParameters, Size targetVideoSize) {
        if (resCoreParameters.filterMode == RESCoreParameters.FILTER_MODE_SOFT) {
            if (resCoreParameters.isPortrait) {
                resCoreParameters.videoHeight = resCoreParameters.previewVideoWidth;
                resCoreParameters.videoWidth = resCoreParameters.previewVideoHeight;
            } else {
                resCoreParameters.videoWidth = resCoreParameters.previewVideoWidth;
                resCoreParameters.videoHeight = resCoreParameters.previewVideoHeight;
            }
        } else {
            float pw, ph, vw, vh;
            if (resCoreParameters.isPortrait) {
                resCoreParameters.videoHeight = targetVideoSize.getWidth();
                resCoreParameters.videoWidth = targetVideoSize.getHeight();
                pw = resCoreParameters.previewVideoHeight;
                ph = resCoreParameters.previewVideoWidth;
            } else {
                resCoreParameters.videoWidth = targetVideoSize.getWidth();
                resCoreParameters.videoHeight = targetVideoSize.getHeight();
                pw = resCoreParameters.previewVideoWidth;
                ph = resCoreParameters.previewVideoHeight;
            }
            vw = resCoreParameters.videoWidth;
            vh = resCoreParameters.videoHeight;
            float pr = ph / pw, vr = vh / vw;
            if (pr == vr) {
                resCoreParameters.cropRatio = 0.0f;
            } else if (pr > vr) {
                resCoreParameters.cropRatio = (1.0f - vr / pr) / 2.0f;
            } else {
                resCoreParameters.cropRatio = -(1.0f - pr / vr) / 2.0f;
            }
        }
    }

    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        videoCore.setVideoEncoder(encoder);
    }

    public void setMirror(boolean isEnableMirror,boolean isEnablePreviewMirror,boolean isEnableStreamMirror) {
        videoCore.setMirror(isEnableMirror,isEnablePreviewMirror,isEnableStreamMirror);
    }
}
