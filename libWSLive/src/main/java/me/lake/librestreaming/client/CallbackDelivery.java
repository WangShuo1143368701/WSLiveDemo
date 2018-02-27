package me.lake.librestreaming.client;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;


public class CallbackDelivery {
    static private CallbackDelivery instance;
    private final Executor mCallbackPoster;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static CallbackDelivery i() {
        return instance == null ? instance = new CallbackDelivery() : instance;
    }

    private CallbackDelivery() {
        mCallbackPoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public void post(Runnable runnable) {
        mCallbackPoster.execute(runnable);
    }

    public void postDelayed(Runnable runnable, long time) {
        handler.postDelayed(runnable,time);
    }
}
