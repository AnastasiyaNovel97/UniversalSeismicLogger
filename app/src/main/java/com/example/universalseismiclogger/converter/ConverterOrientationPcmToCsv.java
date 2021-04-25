package com.example.universalseismiclogger.converter;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import static com.example.universalseismiclogger.shared.DefaultStrings.BASE_FOLDER_PATH;
import static com.example.universalseismiclogger.shared.DefaultStrings.DATA_BUFFER_SIZE_DEFAULT;
import static com.example.universalseismiclogger.shared.DefaultStrings.SAMPLE_RATE;
import static com.example.universalseismiclogger.shared.DefaultStrings.SAMPLE_RATE_DEFAULT;
import static com.example.universalseismiclogger.shared.Extensions.CSV_EXTENSION;
import static com.example.universalseismiclogger.shared.Extensions.PCM_EXTENSION;

public class ConverterOrientationPcmToCsv implements IFormatConverter{
    private String fileName;
    private String outputFilePath;
    private SharedPreferences settings;
    private Context activityContext;
    private Vector<String> outFileNames = new Vector<>();
    private int bufferSize = DATA_BUFFER_SIZE_DEFAULT;

    public ConverterOrientationPcmToCsv(Context activityContext, SharedPreferences settings) {
        this.activityContext = activityContext;
        this.settings = settings;
    }


    @Override
    public String Convert(String filePath, Date startDate, String[] valueNames) {
        byte[] dataBytes = new byte[bufferSize];
        float[] dataFloats = new float[dataBytes.length/4];


        int sampleRate = settings.getInt(SAMPLE_RATE, SAMPLE_RATE_DEFAULT);

        Date currDate = (Date) startDate.clone();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.getDefault());
        long currTime = currDate.getTime();
        float timeForOneSample = (float)1000f / sampleRate; // time in milliseconds used for one sample
        float timeShiftForSamples = 0;

        FileInputStream fInputStream = null;
        FileOutputStream fOutputStreamMicro = null;

        File origFile = new File(filePath);
        String folderPath = origFile.getParent();
        String origName = origFile.getName().replaceFirst("[.][^.]+$", "");
        outputFilePath = folderPath + "/" + origName + CSV_EXTENSION;

        try {
            fInputStream = new FileInputStream(filePath);
            fOutputStreamMicro = new FileOutputStream(outputFilePath);

//            String csvHeader = "Record start date:"+ sdf.format(currDate) + "; Sample rate = " + sampleRate + "\n";
//            fOutputStreamMicro.write(csvHeader.getBytes());

            String csvHeader = "date,time,";
            for(String param: valueNames) { csvHeader += param;}
            csvHeader+="\n";

            fOutputStreamMicro.write(csvHeader.getBytes());

            while ( fInputStream.read(dataBytes) != -1) {
                // to turn bytes to dataFloats as either big endian or little endian.
                ByteBuffer.wrap(dataBytes).order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get(dataFloats);
                final int paramNum = valueNames.length;
                int paramCounter=0;
                String currentLineMicro="";
                for (float sample:
                        dataFloats) {
                    if(paramCounter == 0) {
                        currDate.setTime(currTime + (long) timeShiftForSamples);

                        //magic to generate microseconds
                        currentLineMicro = sdf.format(currDate);
                        currentLineMicro += (int) ((float) (timeShiftForSamples - Math.floor(timeShiftForSamples)) * 1000);

                    }
                    currentLineMicro += "," + String.format("%.5f", sample);
                    paramCounter++;
                    if (paramCounter >= paramNum){
                        currentLineMicro += "\n";
                        fOutputStreamMicro.write(currentLineMicro.getBytes());

                        timeShiftForSamples += timeForOneSample;
                        paramCounter = 0;
                    }


                    //Log.d(TAG, "Convert little endian" + sample);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fInputStream.close();
            fOutputStreamMicro.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFilePath;
    }


    @Override
    public String Convert() {
        return null;
    }

    @Override
    public String getConvertedFilePath() {
        return outputFilePath;
    }
}
