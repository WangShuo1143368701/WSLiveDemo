package me.lake.librestreaming.filter.softaudiofilter;


public class BaseSoftAudioFilter {
    protected int SIZE;
    protected int SIZE_HALF;

    public void onInit(int size) {
        SIZE = size;
        SIZE_HALF = size/2;
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
