package com.example.universalseismiclogger.recorder.interfaces;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

public interface IRecorder {
    IRecorder init(Context activityContext, SharedPreferences settings, int id);
    void startRecorder(String fileName) throws IOException;
    void stopRecorder() throws IOException;
    String getFilePath();
    int getSampleRate();
    int getRecorderType();
}
