package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.universalseismiclogger.shared.ITraceable;

import java.io.IOException;

public class RecorderOrientationSensors implements IRecorder, ITraceable {

    private String filename;
    private int recorderType;

    public RecorderOrientationSensors(int recorderType){
        this.recorderType = recorderType;
    }

    @Override
    public IRecorder init(Context activityContext, SharedPreferences settings) {
        return this;
    }

    @Override
    public void startRecorder(String fileName) {
        this.filename = fileName;
    }

    @Override
    public void stopRecorder() throws IOException {

    }

    @Override
    public String getFilePath() {
        return filename;
    }

    @Override
    public int getSampleRate() {
        return 0;
    }

    @Override
    public int getRecorderType() {
        return recorderType;
    }
}
