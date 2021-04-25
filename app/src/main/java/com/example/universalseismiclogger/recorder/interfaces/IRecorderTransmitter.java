package com.example.universalseismiclogger.recorder.interfaces;

public interface IRecorderTransmitter {
    void SetReceiver(IRecorderReceiver receiver);
    void NotifyReceivers();
}
