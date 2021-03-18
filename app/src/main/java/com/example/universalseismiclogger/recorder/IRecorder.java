package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;

public interface IRecorder {
    IRecorder init(Context activityContext, SharedPreferences settings);
    void startRecorder(String fileName);
    void stopRecorder() throws IOException;
    String getFilePath();
    int getSampleRate();
    int getRecorderType();
}
