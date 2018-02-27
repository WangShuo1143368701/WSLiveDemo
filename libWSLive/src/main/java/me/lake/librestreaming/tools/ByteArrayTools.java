package me.lake.librestreaming.tools;


public class ByteArrayTools {
    public static void intToByteArrayFull(byte[] dst, int pos, int interger) {
        dst[pos] = (byte) ((interger >> 24) & 0xFF);
        dst[pos + 1] = (byte) ((interger >> 16) & 0xFF);
        dst[pos + 2] = (byte) ((interger >> 8) & 0xFF);
        dst[pos + 3] = (byte) ((interger) & 0xFF);
    }

    public static void intToByteArrayTwoByte(byte[] dst, int pos, int interger) {
        dst[pos] = (byte) ((interger >> 8) & 0xFF);
        dst[pos + 1] = (byte) ((interger) & 0xFF);
    }
}
