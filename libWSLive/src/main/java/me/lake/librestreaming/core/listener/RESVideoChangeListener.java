package me.lake.librestreaming.core.listener;


public interface RESVideoChangeListener {
    void onVideoSizeChanged(int width, int height);

    class RESVideoChangeRunable implements Runnable {
        RESVideoChangeListener videoChangeListener;
        int w, h;

        public RESVideoChangeRunable(RESVideoChangeListener videoChangeListener, int w, int h) {
            this.videoChangeListener = videoChangeListener;
            this.w = w;
            this.h = h;
        }

        @Override
        public void run() {
            if (videoChangeListener != null) {
                videoChangeListener.onVideoSizeChanged(w, h);
            }
        }
    }
}