package com.example.universalseismiclogger.uiinteractor;

import android.os.Handler;
import android.os.Looper;

public class UiInteractor implements IThreadAction {
    private static final Handler handler = new Handler(Looper.getMainLooper());

    /** executes the {@code Runnable} on UI Thread. */
    @Override public void execute(Runnable runnable) {
        handler.post(runnable);
    }
}