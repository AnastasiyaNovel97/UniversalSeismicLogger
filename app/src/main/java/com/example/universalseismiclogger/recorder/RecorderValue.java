package com.example.universalseismiclogger.recorder;

public class RecorderValue {

    private static final RecorderValue instance = new RecorderValue();
    private static float value = 0;

    public static RecorderValue GetInstance(){
        return instance;
    }

    private RecorderValue(){};

    public static void SetValue(float input){
        value = input;
    }

    public static float GetValue() {
        return value;
    }
}
