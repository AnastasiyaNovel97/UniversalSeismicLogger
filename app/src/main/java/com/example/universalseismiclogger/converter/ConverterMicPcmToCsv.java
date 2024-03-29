package com.example.universalseismiclogger.converter;

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

import static com.example.universalseismiclogger.shared.DefaultStrings.BASE_FOLDER_PATH;
import static com.example.universalseismiclogger.shared.DefaultStrings.DATA_BUFFER_SIZE_DEFAULT;
import static com.example.universalseismiclogger.shared.Extensions.*;


public class ConverterMicPcmToCsv implements IFormatConverter {

    private String fileName;
    private Date startDate;
    private int sampleRate;
    private String location;
    private int bufferSize = DATA_BUFFER_SIZE_DEFAULT;
    private String outputFilePath;

//    public ConverterMicPcmToCsv(String fileName, Date startDate, int sampleRate) {
//
//        //this.fileName = new File(fileName).getName().replaceFirst("[.][^.]+$", "");
//        this.fileName = fileName;
//        this.startDate = startDate;
//        this.sampleRate = sampleRate;
//    }

    public ConverterMicPcmToCsv(String fileName, Date startDate, int sampleRate, String location) {
        this.fileName = fileName;
        this.startDate = startDate;
        this.sampleRate = sampleRate;
        this.location = location;
    }

    @Override
    public String Convert() {

        byte[] bytes = new byte[bufferSize];
        short[] shorts = new short[bytes.length/2];

        FileInputStream fInputStream = null;
        FileOutputStream fOutputStreamMicro = null;

        File origFile = new File(fileName);
        String folderPath = origFile.getParent();
        String origName = origFile.getName().replaceFirst("[.][^.]+$", "");
        outputFilePath = folderPath + "/" + origName + CSV_EXTENSION;

        int sampleDelay = 1000000 / sampleRate - 1;
        long currentTimeMicro = 0;

        try {
            fInputStream = new FileInputStream(folderPath + "/" + origName + PCM_EXTENSION);
            fOutputStreamMicro = new FileOutputStream(outputFilePath);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            String startDateString = sdf.format(startDate);
            fOutputStreamMicro.write(("time in milliseconds(sampleRate = "+
                    (sampleRate)+", location = "+location+", startDate = "+
                    startDateString+"); mic value\n").getBytes());

            while ( fInputStream.read(bytes) != -1) {
                // to turn bytes to shorts as either big endian or little endian.
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                for (short s:
                     shorts) {
                    String currentLineMillis =(currentTimeMicro/1000)+";"+ s + "\n";
                    currentTimeMicro += sampleDelay;
                    fOutputStreamMicro.write(currentLineMillis.getBytes());
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
//
//
//    @Override
//    public String Convert() {
//
//        byte[] bytes = new byte[bufferSize];
//        short[] shorts = new short[bytes.length/2];
//
//        Date currDate = (Date) startDate.clone();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS", Locale.getDefault());
//        long currTime = currDate.getTime();
//        float timeForOneSample = (float)1000f / sampleRate; // time in milliseconds used for one sample
//        float timeShiftForSamples = 0;
//
//        FileInputStream fInputStream = null;
//        FileOutputStream fOutputStreamMicro = null;
//
//        File origFile = new File(fileName);
//        String folderPath = origFile.getParent();
//        String origName = origFile.getName().replaceFirst("[.][^.]+$", "");
//        outputFilePath = folderPath + "/" + origName + CSV_EXTENSION;
//
//        try {
//            fInputStream = new FileInputStream(folderPath + "/" + origName + PCM_EXTENSION);
//            fOutputStreamMicro = new FileOutputStream(outputFilePath);
//
////            String csvHeader = "Record start date:"+ sdf.format(currDate) + "; Sample rate = " + sampleRate + "\n";
////            fOutputStreamMicro.write(csvHeader.getBytes());
//
//            String csvHeader = "date,time,microphone data value\n";
//            fOutputStreamMicro.write(csvHeader.getBytes());
//
//
//
//            while ( fInputStream.read(bytes) != -1) {
//                // to turn bytes to shorts as either big endian or little endian.
//                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
//                for (short s:
//                     shorts) {
//                    currDate.setTime(currTime + (long)timeShiftForSamples);
//
//                    String currentLine = sdf.format(currDate) + "," + s + "\n";
//
//                    //magic to generate microseconds
//                    String currentLineMicro = sdf.format(currDate);
//                    currentLineMicro += (int)((float) (timeShiftForSamples - Math.floor(timeShiftForSamples)) * 1000);
//                    currentLineMicro += "," + s + "\n";
//
//                    fOutputStreamMicro.write(currentLineMicro.getBytes());
//
//                    timeShiftForSamples += timeForOneSample;
//                    //Log.d(TAG, "Convert little endian" + s);
//                }
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            fInputStream.close();
//            fOutputStreamMicro.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return outputFilePath;
//    }

    @Override
    public String getConvertedFilePath() {
        return null;
    }

    @Override
    public String Convert(String filePath, Date startDate, String[] valueNames) {
        return null;
    }
}
