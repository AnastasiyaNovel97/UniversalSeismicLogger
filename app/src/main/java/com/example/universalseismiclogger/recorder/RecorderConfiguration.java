package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;

public class RecorderConfiguration {

    private static RecorderConfiguration instance;

    private SharedPreferences settings;

    private Context parentContext;
    private int sampleRate;
    private boolean isAudioUnprocessed;

    private RecorderConfiguration(){}

    public static RecorderConfiguration getInstance(){
        if(instance == null){
            instance = new RecorderConfiguration();
        }
        return instance;
    }

    public Context getContext(){
        return parentContext;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public boolean isAudioUnprocessed() {
        return isAudioUnprocessed;
    }


    public void setParentContext(Context parentContext) {
        this.parentContext = parentContext;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setAudioUnprocessed(boolean audioUnprocessed) {
        isAudioUnprocessed = audioUnprocessed;
    }

    public void InitConfiguration(Context parentContext, SharedPreferences preferences){
        this.parentContext = parentContext;
        this.settings = preferences;
    }
}
