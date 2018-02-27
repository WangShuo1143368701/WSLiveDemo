package me.lake.librestreaming.core;

import android.graphics.ImageFormat;
import android.hardware.Camera;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.tools.LogTools;


public class CameraHelper {
    public static int targetFps = 30000;
    private static int[] supportedSrcVideoFrameColorType = new int[]{ImageFormat.NV21, ImageFormat.YV12};

    public static boolean configCamera(Camera camera, RESCoreParameters coreParameters) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            }
        }
        parameters.setPreviewSize(coreParameters.previewVideoWidth, coreParameters.previewVideoHeight);
        parameters.setPreviewFpsRange(coreParameters.previewMinFps, coreParameters.previewMaxFps);
        try {
            camera.setParameters(parameters);
        } catch (Exception e) {
            camera.release();
            return false;
        }
        return true;
    }

    public static void selectCameraFpsRange(Camera.Parameters parameters, RESCoreParameters coreParameters) {
        List<int[]> fpsRanges = parameters.getSupportedPreviewFpsRange();
        Collections.sort(fpsRanges, new Comparator<int[]>() {
            @Override
            public int compare(int[] lhs, int[] rhs) {
                int r = Math.abs(lhs[0] - targetFps) + Math.abs(lhs[1] - targetFps);
                int l = Math.abs(rhs[0] - targetFps) + Math.abs(rhs[1] - targetFps);
                if (r > l) {
                    return 1;
                } else if (r < l) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        coreParameters.previewMinFps = fpsRanges.get(0)[0];
        coreParameters.previewMaxFps = fpsRanges.get(0)[1];
    }

    public static void selectCameraPreviewWH(Camera.Parameters parameters, RESCoreParameters coreParameters, Size targetSize) {
        List<Camera.Size> previewsSizes = parameters.getSupportedPreviewSizes();
        Collections.sort(previewsSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if ((lhs.width * lhs.height) > (rhs.width * rhs.height)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        for (Camera.Size size : previewsSizes) {
            if (size.width >= targetSize.getWidth() && size.height >= targetSize.getHeight()) {
                coreParameters.previewVideoWidth = size.width;
                coreParameters.previewVideoHeight = size.height;
                return;
            }
        }
    }

    public static boolean selectCameraColorFormat(Camera.Parameters parameters, RESCoreParameters coreParameters) {
        List<Integer> srcColorTypes = new LinkedList<>();
        List<Integer> supportedPreviewFormates = parameters.getSupportedPreviewFormats();
        for (int colortype : supportedSrcVideoFrameColorType) {
            if (supportedPreviewFormates.contains(colortype)) {
                srcColorTypes.add(colortype);
            }
        }
        //select preview colorformat
        if (srcColorTypes.contains(coreParameters.previewColorFormat = ImageFormat.NV21)) {
            coreParameters.previewColorFormat = ImageFormat.NV21;
        } else if ((srcColorTypes.contains(coreParameters.previewColorFormat = ImageFormat.YV12))) {
            coreParameters.previewColorFormat = ImageFormat.YV12;
        } else {
            LogTools.e("!!!!!!!!!!!UnSupport,previewColorFormat");
            return false;
        }
        return true;
    }
}
