package com.wangshuo.wslive.wslivedemo;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.FishEyeFilterHard;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

/**
 * Created by WangShuo on 2018/2/26.
 */

public class LiveUI implements View.OnClickListener {

    private LiveActivity activity;
    private StreamLiveCameraView liveCameraView;
    private String rtmpUrl = "";
    boolean isFilter = false;
    boolean isMirror = false;

    private Button btnStartStreaming;
    private Button btnStopStreaming;
    private Button btnStartRecord;
    private Button btnStopRecord;
    private Button btnFliter;
    private Button btnSwapCamera;
    private Button btnScreenshot;
    private Button btnMirror;

    private ImageView imageView;

    public LiveUI(LiveActivity liveActivity , StreamLiveCameraView liveCameraView , String rtmpUrl) {
        this.activity = liveActivity;
        this.liveCameraView = liveCameraView;
        this.rtmpUrl = rtmpUrl;

        init();
    }

    private void init() {
        btnStartStreaming = (Button) activity.findViewById(R.id.btn_startStreaming);
        btnStartStreaming.setOnClickListener(this);

        btnStopStreaming = (Button) activity.findViewById(R.id.btn_stopStreaming);
        btnStopStreaming.setOnClickListener(this);

        btnStartRecord = (Button) activity.findViewById(R.id.btn_startRecord);
        btnStartRecord.setOnClickListener(this);

        btnStopRecord = (Button) activity.findViewById(R.id.btn_stopRecord);
        btnStopRecord.setOnClickListener(this);

        btnFliter = (Button) activity.findViewById(R.id.btn_filter);
        btnFliter.setOnClickListener(this);

        btnSwapCamera = (Button) activity.findViewById(R.id.btn_swapCamera);
        btnSwapCamera.setOnClickListener(this);

        btnScreenshot = (Button) activity.findViewById(R.id.btn_screenshot);
        btnScreenshot.setOnClickListener(this);

        btnMirror = (Button) activity.findViewById(R.id.btn_mirror);
        btnMirror.setOnClickListener(this);

        imageView = (ImageView) activity.findViewById(R.id.iv_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(View view) {
       switch (view.getId()){
           case R.id.btn_startStreaming://开始推流
               if(!liveCameraView.isStreaming()){
                   liveCameraView.startStreaming(rtmpUrl);
               }
               break;
           case R.id.btn_stopStreaming://停止推流
               if(liveCameraView.isStreaming()){
                   liveCameraView.stopStreaming();
               }
               break;
           case R.id.btn_startRecord://开始录制
               if(!liveCameraView.isRecord()){
                   Toast.makeText(activity,"开始录制视频",Toast.LENGTH_SHORT).show();
                   liveCameraView.startRecord();
               }
               break;
           case R.id.btn_stopRecord://停止录制
               if(liveCameraView.isRecord()){
                   liveCameraView.stopRecord();
                   Toast.makeText(activity,"视频已成功保存至系统根目录的 Movies/WSLive文件夹中",Toast.LENGTH_LONG).show();
               }
               break;
           case R.id.btn_filter://切换滤镜
               BaseHardVideoFilter baseHardVideoFilter = null;
               if(isFilter){
                   baseHardVideoFilter = new GPUImageCompatibleFilter(new GPUImageBeautyFilter());
               }else {
                   baseHardVideoFilter = new FishEyeFilterHard();
               }
               liveCameraView.setHardVideoFilter(baseHardVideoFilter);
               isFilter = !isFilter;
               break;
           case R.id.btn_swapCamera://切换摄像头
               liveCameraView.swapCamera();
               break;
           case R.id.btn_screenshot://截帧
               liveCameraView.takeScreenShot(new RESScreenShotListener() {
                   @Override
                   public void onScreenShotResult(Bitmap bitmap) {
                       if(bitmap != null){
                           imageView.setVisibility(View.VISIBLE);
                           imageView.setImageBitmap(bitmap);
                       }

                   }
               });
               break;
           case R.id.btn_mirror://镜像
               if(isMirror){
                   liveCameraView.setMirror(true,false,false);
               }else {
                   liveCameraView.setMirror(true,true,true);
               }
               isMirror = !isMirror;
               break;
           default:
               break;
       }
    }
}
