package com.example.universalseismiclogger.shared;

import android.os.Environment;

public class DefaultStrings {
    public final static String BASE_FOLDER_PATH = Environment.getExternalStorageDirectory()
            + "/UniversalSeismicLogger/";


    public final static String RECORDER_CONFIG = "RecorderConfig";

    public final static String GPS_LOCATION = "GpsLocation";
    public final static String GPS_LOCATION_DEFAULT = "none";

    public final static String SAMPLE_RATE_POSITION = "SampleRatePosition";
    public final static String LOG_NAME = "LogName";
    public final static String USE_MIC = "UseMic";
    public final static String UNPROCESSED_MIC = "UnprocessedMic";
    public final static String USE_GYROSCOPE = "UseGyroscope";
    public final static String USE_ACCELEROMETER = "UseAccelerometer";
    public final static String USE_COMPASS = "UseCompass";
    public final static String SAMPLE_RATE = "SampleRate";


    public final static int MANAGER_ID = -1;
    public final static int MICROPHONE_ID = 0;
    public final static int GYROSCOPE_ID = 1;
    public final static int ACCELEROMETER_ID = 2;
    public final static int COMPASS_ID = 3;



}
