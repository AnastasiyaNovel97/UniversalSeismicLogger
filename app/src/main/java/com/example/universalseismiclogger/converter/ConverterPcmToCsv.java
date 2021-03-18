package com.example.universalseismiclogger.converter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.example.universalseismiclogger.shared.DefaultStrings.BASE_FOLDER_PATH;
import static com.example.universalseismiclogger.shared.Extensions.*;


public class ConverterPcmToCsv implements IFormatConverter {

    private String fileName;
    private Date startDate;
    private int sampleRate;
    private String location;
    private int bufferSize = 1024;

    public ConverterPcmToCsv(String fileName, Date startDate, int sampleRate) {
        this.fileName = fileName;
        this.startDate = startDate;
        this.sampleRate = sampleRate;
    }

    public ConverterPcmToCsv(String fileName, Date startDate, int sampleRate, String location) {
        this.fileName = fileName;
        this.startDate = startDate;
        this.sampleRate = sampleRate;
        this.location = location;
    }

    @Override
    public String Convert() {
        byte[] bytes = new byte[bufferSize];
        short[] shorts = new short[bytes.length/2];

        Date currDate = (Date) startDate.clone();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.getDefault());
        long currTime = currDate.getTime();
        float timeForOneSample = (float)1000f / sampleRate; // time in milliseconds used for one sample
        float timeShiftForSamples = 0;

        FileInputStream fInputStream = null;
        FileOutputStream fOutputStream = null;
        FileOutputStream fOutputStream1 = null;
        FileOutputStream fOutputStreamNano = null;

        try {
            fInputStream = new FileInputStream(BASE_FOLDER_PATH + fileName +"/"+ fileName + PCM_EXTENSION);
            fOutputStream = new FileOutputStream(BASE_FOLDER_PATH + fileName +"/"+ fileName + CSV_EXTENSION);
            fOutputStream1 = new FileOutputStream(BASE_FOLDER_PATH + fileName +"/"+ fileName + "withoutTime" + CSV_EXTENSION);
            fOutputStreamNano = new FileOutputStream(BASE_FOLDER_PATH + fileName +"/"+ fileName + "timeNano" + CSV_EXTENSION);

            String csvHeader = "Record start date:"+ sdf.format(currDate) + "; Sample rate = " + sampleRate + "\n";
            fOutputStream.write(csvHeader.getBytes());
            fOutputStream1.write(csvHeader.getBytes());
            fOutputStreamNano.write(csvHeader.getBytes());

            while ( fInputStream.read(bytes) != -1) {
                // to turn bytes to shorts as either big endian or little endian.
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                for (short s:
                     shorts) {
                    currDate.setTime(currTime + (long)timeShiftForSamples);

                    String currentLine = sdf.format(currDate) + "," + s + "\n";

                    //magic to generate nanoseconds
                    String currentLineNano = sdf.format(currDate);
                    float nanoShift = (float) (timeShiftForSamples - Math.floor(timeShiftForSamples));
                    currentLineNano += (int)(nanoShift * 1000000);
                    currentLineNano += "," + s + "\n";

                    fOutputStream.write(currentLine.getBytes());
                    fOutputStreamNano.write(currentLineNano.getBytes());
                    fOutputStream1.write((s + "\n").getBytes());

                    timeShiftForSamples += timeForOneSample;
                    //Log.d(TAG, "Convert little endian" + s);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fInputStream.close();
            fOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }
}
