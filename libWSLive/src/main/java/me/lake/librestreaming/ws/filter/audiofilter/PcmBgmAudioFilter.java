package me.lake.librestreaming.ws.filter.audiofilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import me.lake.librestreaming.filter.softaudiofilter.BaseSoftAudioFilter;


public class PcmBgmAudioFilter extends BaseSoftAudioFilter {
    FileInputStream fis;
    String filePath;
    byte[] bgm;

    public PcmBgmAudioFilter(String filepath) {
        filePath = filepath;
    }

    @Override
    public void onInit(int size) {
        super.onInit(size);
        try {
            fis = new FileInputStream(new File(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fis.mark(fis.available());
        } catch (IOException e) {
        }
        bgm = new byte[SIZE];
    }

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        try {
            if (fis.read(bgm, 0, SIZE) < SIZE) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < SIZE; i += 2) {
            short origin = (short) (((orignBuff[i + 1] << 8) | orignBuff[i] & 0xff));
            short bg = (short) (((bgm[i + 1] << 8) | bgm[i] & 0xff));
            bg /= 32;
            origin *=4;
            short res = (short) (origin + bg);
            targetBuff[i + 1] = (byte) (res >> 8);
            targetBuff[i] = (byte) (res);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
