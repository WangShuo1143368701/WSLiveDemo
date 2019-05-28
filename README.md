# WSLiveDemo
直播SDK，推流，录制视频，滤镜。

博客教程：http://blog.csdn.net/King1425/article/details/79392158<br/>
交流群号：364886309 ，949792060

概述：
---
**现在把我们项目中的直播SDK开源出来，我们是境外直播平台，百万用户，经过半年迭代，SDK已经相当稳定，大家可以放心使用。https://github.com/WangShuo1143368701/WSLiveDemo**
这个sdk是我根据这个[librestreaming](https://github.com/lakeinchina/librestreaming)修修改改出来的，由于改了太多的代码，用法已经不一样了。

之前写过一篇[ffmpeg实战教程（十一）手把手教你实现直播功能，不依赖第三方SDK](http://blog.csdn.net/king1425/article/details/72560673)
是用ffmpeg实现的推流，但是在实际移动端直播项目中，推流是不适合用ffmpeg的。

特性：
--
支持视频录制和推流，推流录制视频可以同时进行<br/>
支持推流过程中实时截帧功能<br/>
支持推流过程中实时镜像功能，不会打断推流<br/>
支持设置关键帧间隔gop<br/>
支持动态设置码率，帧率<br/>
支持分开设置预览分辨率，编码的分辨率<br/>
支持gpu滤镜，并可以通过opengles绘制图像纹理来自定义滤镜。<br/>
支持设fbo滤镜组。<br/>
支持设置水印<br/>
支持前后摄像头快速切换，不会打断推流。<br/>
支持后台推流，后台录制视频<br/>
美颜滤镜可动态调节磨皮，美白，红润。<br/>
兼容GPUImage，一行代码不用修改就可以直接使用GPUImage的滤镜。可参考demo。<br/>

关于美颜：
----
美颜滤镜可动态调节磨皮，美白，红润。你可以调出一个你喜欢的美颜滤镜。

关于截帧，镜像：
----
推流过程中可以实时截帧。<br/>
推流过程中可以实时调节镜像，不会打断推流。可以分别调节预览镜像，推流镜像。

关于性能：
-----
采用相机回调纹理texture,OpenGL渲染后直接把textureID传给编码器的方案，中间没有数据格式转换，没有glReadPixel()函数耗时问题。所以性能较其它方案要好的多。缺点是必须是Android4.3以上。

关于拉流：
-----
https://github.com/WangShuo1143368701/WS_IJK
优化过的IJK播放器，秒开实现，推流端断网回调等。。。

关于使用：
-----

1.所有常用API都在StreamLiveCameraView类中

```
 <me.lake.librestreaming.ws.StreamLiveCameraView
        android:id="@+id/stream_previewView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
```

2.初始化推流配置， StreamAVOption类里面有多种参数可配置，如不配置则使用默认值

```
    /**
     * 设置推流参数
     */
    public void initLiveConfig() {
        mLiveCameraView = (StreamLiveCameraView) findViewById(R.id.stream_previewView);

        //参数配置 start   
        streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl = rtmpUrl;
        //参数配置 end

        mLiveCameraView.init(this, streamAVOption);
        mLiveCameraView.addStreamStateListener(resConnectionListener);
        //设置滤镜组
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        files.add(new GPUImageCompatibleFilter(new GPUImageAddBlendFilter()));
        mLiveCameraView.setHardVideoFilter(new HardVideoGroupFilter(files));
    }
```
3.开始推流录制 具体参考demo：

```
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
                   liveCameraView.startRecord();
               }
               break;
           case R.id.btn_stopRecord://停止录制
               if(liveCameraView.isRecord()){
                   liveCameraView.stopRecord();                
               }
```

关于集成：
-----
方法1.https://github.com/WangShuo1143368701/WSLiveDemo下载后，copy出libWSLive库到你的项目中即可。

方法2.

```
//Add it in your root build.gradle at the end of repositories:
 
 allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
 }

    //Add the dependency

   dependencies {
	         compile 'com.github.WangShuo1143368701:WSLiveDemo:v1.7'
	}

```

关于学习：
-----
欢迎加入音视频交流群：364886309，949792060







