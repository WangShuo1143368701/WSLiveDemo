package me.lake.librestreaming.core;


public class RESFrameRateMeter {
    private static final long TIMETRAVEL = 1;
    private static final long TIMETRAVEL_MS = TIMETRAVEL * 1000;
    private static final long GET_TIMETRAVEL_MS = 2 * TIMETRAVEL_MS;
    private int times;
    private float lastFps;
    private long lastUpdateTime;

    public RESFrameRateMeter() {
        times = 0;
        lastFps = 0;
        lastUpdateTime = 0;
    }

    public void count() {
        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = now;
        }
        if ((now - lastUpdateTime) > TIMETRAVEL_MS) {
            lastFps = (((float) times) / (now - lastUpdateTime)) * 1000.0f;
            lastUpdateTime = now;
            times = 0;
        }
        ++times;
    }

    public float getFps() {
        if ((System.currentTimeMillis() - lastUpdateTime) > GET_TIMETRAVEL_MS) {
            return 0;
        } else {
            return lastFps;
        }
    }

    public void reSet() {
        times = 0;
        lastFps = 0;
        lastUpdateTime = 0;
    }
}
