package me.lake.librestreaming.model;


import android.hardware.Camera;


public class RESConfig {

    public static class FilterMode {
        public static final int HARD = RESCoreParameters.FILTER_MODE_HARD;
        public static final int SOFT = RESCoreParameters.FILTER_MODE_SOFT;
    }

    public static class RenderingMode {
        public static final int NativeWindow = RESCoreParameters.RENDERING_MODE_NATIVE_WINDOW;
        public static final int OpenGLES = RESCoreParameters.RENDERING_MODE_OPENGLES;
    }

    public static class DirectionMode {
        public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = RESCoreParameters.FLAG_DIRECTION_FLIP_HORIZONTAL;
        public static final int FLAG_DIRECTION_FLIP_VERTICAL = RESCoreParameters.FLAG_DIRECTION_FLIP_VERTICAL;
        public static final int FLAG_DIRECTION_ROATATION_0 = RESCoreParameters.FLAG_DIRECTION_ROATATION_0;
        public static final int FLAG_DIRECTION_ROATATION_90 = RESCoreParameters.FLAG_DIRECTION_ROATATION_90;
        public static final int FLAG_DIRECTION_ROATATION_180 = RESCoreParameters.FLAG_DIRECTION_ROATATION_180;
        public static final int FLAG_DIRECTION_ROATATION_270 = RESCoreParameters.FLAG_DIRECTION_ROATATION_270;
    }

    private int filterMode;
    private Size targetVideoSize;
    private int videoBufferQueueNum;
    private int bitRate;
    private String rtmpAddr;
    private int renderingMode;
    private int defaultCamera;
    private int frontCameraDirectionMode;
    private int backCameraDirectionMode;
    private int videoFPS;
    private int videoGOP;
    private boolean printDetailMsg;
    private Size targetPreviewSize;

    private RESConfig() {
    }

    public static RESConfig obtain() {
        RESConfig res = new RESConfig();
        res.setFilterMode(FilterMode.SOFT);
        res.setRenderingMode(RenderingMode.NativeWindow);
        res.setTargetVideoSize(new Size(1280, 720));
        res.setVideoFPS(15);
        res.setVideoGOP(2);
        res.setVideoBufferQueueNum(5);
        res.setBitRate(2000000);
        res.setPrintDetailMsg(false);
        res.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        res.setBackCameraDirectionMode(DirectionMode.FLAG_DIRECTION_ROATATION_0);
        res.setFrontCameraDirectionMode(DirectionMode.FLAG_DIRECTION_ROATATION_0);
        return res;
    }

        /**
         * set the filter mode.
         *
         * @param filterMode {@link FilterMode}
         */

    public void setFilterMode(int filterMode) {
        this.filterMode = filterMode;
    }

    /**
     * set the default camera to start stream
     */
    public void setDefaultCamera(int defaultCamera) {
        this.defaultCamera = defaultCamera;
    }

    /**
     * set front camera rotation & flip
     *
     * @param frontCameraDirectionMode {@link DirectionMode}
     */
    public void setFrontCameraDirectionMode(int frontCameraDirectionMode) {
        this.frontCameraDirectionMode = frontCameraDirectionMode;
    }

    /**
     * set front camera rotation & flip
     *
     * @param backCameraDirectionMode {@link DirectionMode}
     */
    public void setBackCameraDirectionMode(int backCameraDirectionMode) {
        this.backCameraDirectionMode = backCameraDirectionMode;
    }

    /**
     * set  renderingMode when using soft mode<br/>
     * no use for hard mode
     *
     * @param renderingMode {@link RenderingMode}
     */
    public void setRenderingMode(int renderingMode) {
        this.renderingMode = renderingMode;
    }

    /**
     * no use for now
     *
     * @param printDetailMsg
     */
    public void setPrintDetailMsg(boolean printDetailMsg) {
        this.printDetailMsg = printDetailMsg;
    }

    /**
     * set the target video size.<br/>
     * real video size may different from it.Depend on device.
     *
     * @param videoSize
     */
    public void setTargetVideoSize(Size videoSize) {
        targetVideoSize = videoSize;
    }

    /**
     * set video buffer number for soft mode.<br/>
     * num larger:video Smoother,more memory.
     *
     * @param num
     */
    public void setVideoBufferQueueNum(int num) {
        videoBufferQueueNum = num;
    }

    /**
     * set video bitrate
     *
     * @param bitRate
     */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getVideoFPS() {
        return videoFPS;
    }

    public void setVideoFPS(int videoFPS) {
        this.videoFPS = videoFPS;
    }

    public int getVideoGOP(){
        return videoGOP;
    }

    public void setVideoGOP(int videoGOP){
        this.videoGOP = videoGOP;
    }

    public int getVideoBufferQueueNum() {
        return videoBufferQueueNum;
    }

    public int getBitRate() {
        return bitRate;
    }

    public Size getTargetVideoSize() {
        return targetVideoSize;
    }

    public int getFilterMode() {
        return filterMode;
    }

    public int getDefaultCamera() {
        return defaultCamera;
    }

    public int getBackCameraDirectionMode() {
        return backCameraDirectionMode;
    }

    public int getFrontCameraDirectionMode() {
        return frontCameraDirectionMode;
    }

    public int getRenderingMode() {
        return renderingMode;
    }

    public String getRtmpAddr() {
        return rtmpAddr;
    }

    public void setRtmpAddr(String rtmpAddr) {
        this.rtmpAddr = rtmpAddr;
    }

    public boolean isPrintDetailMsg() {
        return printDetailMsg;
    }

    public void setTargetPreviewSize(Size previewSize) {
        targetPreviewSize = previewSize;
    }

    public Size getTargetPreviewSize() {
        return targetPreviewSize;
    }

}
