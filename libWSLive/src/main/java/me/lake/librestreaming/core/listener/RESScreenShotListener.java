package me.lake.librestreaming.core.listener;

import android.graphics.Bitmap;


public interface RESScreenShotListener {
    void onScreenShotResult(Bitmap bitmap);

    class RESScreenShotListenerRunable implements Runnable {
        Bitmap resultBitmap;
        RESScreenShotListener resScreenShotListener;

        public RESScreenShotListenerRunable(RESScreenShotListener listener, Bitmap bitmap) {
            resScreenShotListener = listener;
            resultBitmap = bitmap;
        }

        @Override
        public void run() {
            if (resScreenShotListener != null) {
                resScreenShotListener.onScreenShotResult(resultBitmap);
            }
        }
    }
}