package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.Vector;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;

public class RecorderManager implements IRecorder {

    private Vector<IRecorder> dataRecorders = new Vector<>();

//    public void AddRecorder(IRecorder newRecorder){
//        dataRecorders.add(newRecorder);
//    }


    @Override
    public IRecorder init(Context activityContext, SharedPreferences settings) {
        dataRecorders = new Vector<>();

        if(settings.getBoolean(USE_MIC, false)){
            dataRecorders.add((new RecorderMicToWav()).init(activityContext, settings));
        }

        if(settings.getBoolean(USE_GYROSCOPE, false)){

        }
        return this;
    }


    @Override
    public void startRecorder(String fileName) {

    }

    @Override
    public void stopRecorder() {

    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public int getSampleRate() {
        return 0;
    }

    @Override
    public int getRecorderType() {
        return MANAGER_ID;
    }
}
