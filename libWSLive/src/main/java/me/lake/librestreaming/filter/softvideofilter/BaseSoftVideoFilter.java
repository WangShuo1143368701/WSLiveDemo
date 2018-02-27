package me.lake.librestreaming.filter.softvideofilter;


public class BaseSoftVideoFilter {
    protected int SIZE_WIDTH;
    protected int SIZE_HEIGHT;
    protected int SIZE_Y;
    protected int SIZE_TOTAL;
    protected int SIZE_U;
    protected int SIZE_UV;

    public void onInit(int VWidth, int VHeight) {
        SIZE_WIDTH = VWidth;
        SIZE_HEIGHT = VHeight;
        SIZE_Y = SIZE_HEIGHT * SIZE_WIDTH;
        SIZE_UV = SIZE_HEIGHT * SIZE_WIDTH / 2;
        SIZE_U = SIZE_UV / 2;
        SIZE_TOTAL = SIZE_Y * 3 / 2;
    }

    /**
     *
     * @param orignBuff
     * @param targetBuff
     * @param presentationTimeMs
     * @param sequenceNum
     * @return false to use orignBuff,true to use targetBuff
     */
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        return false;
    }

    public void onDestroy() {

    }
}
