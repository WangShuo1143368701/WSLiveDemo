package me.lake.librestreaming.tools;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by WangShuo on 2017/6/30.
 */

public class CameraUtil {
    private static final String tag = "wangshuo";
    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private CameraSizeComparator2 sizeComparator2 = new CameraSizeComparator2();
    private static CameraUtil cameraUtil = null;
    public static boolean hasSupportedFrontVideoSizes = true;
    private CameraUtil(){

    }
    public static CameraUtil getInstance(){
        if(cameraUtil == null){
            cameraUtil = new CameraUtil();
            return cameraUtil;
        }
        else{
            return cameraUtil;
        }
    }

    public Size getBestSize(List<Size> list, int th){
        if(list == null || list.size() < 1){
            return null;
        }
        boolean bool= false;

        Collections.sort(list, sizeComparator2);
        int i = 0;
        for(Size s:list){
            if((s.width < th) && (s.width > 350) && equalRate(s, 1.7777f)){
                Log.i(tag, "最终设置Video尺寸:w = " + s.width + "h = " + s.height);
                bool = true;
                break;
            }
            i++;
        }
        if(bool){
            return list.get(i);
        }
        return null;
    }

    public Size getBestPreviewSize(List<Size> list, int th){
        if(list == null || list.size() < 1){
            return null;
        }
        boolean bool= false;

        Collections.sort(list, sizeComparator);
        int i = 0;
        for(Size s:list){
            if((s.width > th) && equalRate(s, 1.7777f)){
                Log.i(tag, "最终设置预览尺寸:w = " + s.width + "h = " + s.height);
                bool = true;
                break;
            }
            i++;
        }
        if(bool){
            return list.get(i);
        }
        return null;
    }

    public boolean equalRate(Size s, float rate){
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.2)
        {
            return true;
        }
        else{
            return false;
        }
    }

    public  class CameraSizeComparator implements Comparator<Size> {
        //按升序排列
        public int compare(Size lhs, Size rhs) {
            // TODO Auto-generated method stub
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width > rhs.width){
                return 1;
            }
            else{
                return -1;
            }
        }

    }

    public  class CameraSizeComparator2 implements Comparator<Size> {
        //按降序排列
        public int compare(Size lhs, Size rhs) {
            // TODO Auto-generated method stub
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width < rhs.width){
                return 1;
            }
            else{
                return -1;
            }
        }

    }

    public static List<Size> getBackCameraPreviewSize(){
        Camera back = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        List<Size> backSizeList = back.getParameters().getSupportedPreviewSizes();
        back.release();
        return backSizeList;
    }

    public static List<Size> getFrontCameraPreviewSize(){
        Camera front = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        List<Size> frontSizeList = front.getParameters().getSupportedPreviewSizes();
        front.release();
        return frontSizeList;
    }

    public static List<Size> getBackCameraVideoSize(){
        Camera back = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        List<Size> backSizeList = back.getParameters().getSupportedVideoSizes();
        back.release();
        return backSizeList;
    }

    public static List<Size> getFrontCameraVideoSize(){
        Camera front = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        List<Size> frontSizeList = front.getParameters().getSupportedVideoSizes();
        front.release();
        return frontSizeList;
    }

    public static List<Size> getFrontCameraSize(){
        Camera front = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        List<Size> frontSizeList = front.getParameters().getSupportedVideoSizes();
        if(null == frontSizeList || frontSizeList.size()<=0){
            frontSizeList = front.getParameters().getSupportedPreviewSizes();
            hasSupportedFrontVideoSizes = false;
            Log.e(tag,"getSupportedVideoSizes==null");
        }
        front.release();
        return frontSizeList;
    }

    /**
     * 判断当前设备是手机还是平板，代码来自 Google I/O App for Android
     * @param context
     * @return 平板返回 True，手机返回 False
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
