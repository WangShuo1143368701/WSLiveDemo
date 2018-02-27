package me.lake.librestreaming.ws;

/**
 * Created by WangShuo on 2017/6/11.
 */

public class StreamAVOption {

    public int cameraIndex = StreamConfig.AVOptionsHolder.DEFAULT_CAMERA_INDEX;//前后置摄像头
    public int previewWidth = StreamConfig.AVOptionsHolder.DEFAULT_PREVIEW_WIDTH;//预览宽
    public int previewHeight = StreamConfig.AVOptionsHolder.DEFAULT_PREVIEW_HEIGHT;//预览高
    public int videoWidth = StreamConfig.AVOptionsHolder.DEFAULT_VIDEO_WIDTH;//推流的视频宽
    public int videoHeight = StreamConfig.AVOptionsHolder.DEFAULT_VIDEO_HEIGHT;//推流的视频高
    public int videoBitrate = StreamConfig.AVOptionsHolder.DEFAULT_VIDEO_BITRATE;//比特率
    public int videoFramerate = StreamConfig.AVOptionsHolder.DEFAULT_VIDEO_FPS;//帧率
    public int videoGOP = StreamConfig.AVOptionsHolder.DEFAULT_VIDEO_GOP;//gop 关键帧间隔
    public String streamUrl = "";

    public static int recordVideoWidth = 544;//录制的视频宽
    public static int recordVideoHeight = 960;//录制的视频高
}
