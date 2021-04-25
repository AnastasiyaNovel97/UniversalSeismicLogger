package com.example.universalseismiclogger.recorder.interfaces;

public interface IRecorderReceiver {
    void Update(int recorderId, float[] data);
}
