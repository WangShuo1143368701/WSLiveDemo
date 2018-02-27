package me.lake.librestreaming.core.listener;


public interface RESConnectionListener {
    void onOpenConnectionResult(int result);

    void onWriteError(int errno);

    void onCloseConnectionResult(int result);

    class RESWriteErrorRunable implements Runnable {
        RESConnectionListener connectionListener;
        int errno;

        public RESWriteErrorRunable(RESConnectionListener connectionListener, int errno) {
            this.connectionListener = connectionListener;
            this.errno = errno;
        }

        @Override
        public void run() {
            if (connectionListener != null) {
                connectionListener.onWriteError(errno);
            }
        }
    }
}