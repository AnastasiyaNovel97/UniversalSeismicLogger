package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.universalseismiclogger.converter.ConverterMicPcmToCsv;
import com.example.universalseismiclogger.converter.ConverterOrientationPcmToCsv;
import com.example.universalseismiclogger.csvparcer.CsvFile;
import com.example.universalseismiclogger.csvparcer.CsvMerger;
import com.example.universalseismiclogger.filescanner.FileScanner;
import com.example.universalseismiclogger.recorder.interfaces.IRecorder;
import com.example.universalseismiclogger.recorder.interfaces.IRecorderReceiver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;
import static com.example.universalseismiclogger.shared.Extensions.CSV_EXTENSION;
import static com.example.universalseismiclogger.shared.LogTags.MY_LOGS;

public class RecorderManager implements IRecorder, IRecorderReceiver {

    private Vector<IRecorder> dataRecorders = new Vector<>();
    private String outFilePath;
    private int sampleRate;
    private String recordFileName;
    private String recordFolderPath;
    private String recordFullPath;
    private Date dateStart;
    private Context activityContext;
    private SharedPreferences settings;
    private boolean isReading;
    private volatile String micCsvPath = null;

    public void SetDate(Date dateNow){
        dateStart = dateNow;
    }

    public IRecorder init(Context activityContext, SharedPreferences settings){
        return init(activityContext,settings,0);
    }

    @Override
    public IRecorder init(Context activityContext, SharedPreferences settings, int id) {

        sampleRate = settings.getInt(SAMPLE_RATE, SAMPLE_RATE_DEFAULT);

        this.activityContext = activityContext;
        this.settings = settings;

        initRecorders();

        return this;
    }

    private void initRecorders(){
        int idRecorder = 0;
        dataRecorders = new Vector<>();

        if(settings.getBoolean(USE_MIC, false)){
            dataRecorders.add((new RecorderMic()).init(activityContext, settings, idRecorder++));
        }

        if(settings.getBoolean(USE_GYROSCOPE, false)){
            dataRecorders.add((new RecorderOrientationSensors(GYROSCOPE_ID)).init(activityContext, settings, idRecorder++));
        }

        if(settings.getBoolean(USE_ACCELEROMETER, false)){
            dataRecorders.add((new RecorderOrientationSensors(ACCELEROMETER_ID)).init(activityContext, settings, idRecorder++));
        }

        if(settings.getBoolean(USE_COMPASS, false)){
            dataRecorders.add((new RecorderOrientationSensors(COMPASS_ID)).init(activityContext, settings, idRecorder++));
        }
    }

    private String generateOutFilePath(String fileName){
        recordFileName = fileName + "_all"+ MANAGER_ID;
        recordFolderPath = BASE_FOLDER_PATH + fileName +"/";
        recordFullPath = recordFolderPath + recordFileName + CSV_EXTENSION;
        return recordFullPath;
    }


    @Override
    public void startRecorder(String fileName) {
        initRecorders();
        isReading = true;
        String outPath = generateOutFilePath(fileName);
        for (IRecorder recorder :
                dataRecorders) {
            try {
                recorder.startRecorder(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int a = 0;
    }

    @Override
    public void stopRecorder() {
        isReading = false;
        Vector<CsvFile> csvFiles = new Vector<>();
        for (IRecorder recorder :
                dataRecorders) {
            try {
                recorder.stopRecorder();

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                if(recorder.getRecorderType() == MICROPHONE_ID) {
                    micCsvPath = null;
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            ConverterMicPcmToCsv converterPcmToCsv = new ConverterMicPcmToCsv(recorder.getFilePath(),
                                    dateStart, sampleRate, settings.getString(GPS_LOCATION, GPS_LOCATION_DEFAULT));

                            micCsvPath = converterPcmToCsv.Convert();
                        }
                    });

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String micCP = null;
                    while (micCsvPath == null){
                        micCP = micCsvPath;
                    }
                    csvFiles.add(new CsvFile(new FileInputStream(micCsvPath)));
                }
                else {
                    csvFiles.add(new CsvFile(new FileInputStream(recorder.getFilePath())));
//                    executorService.submit(new Runnable() {
//                        @Override
//                        public void run() {
//                            ConverterOrientationPcmToCsv converterPcmToCsv = new ConverterOrientationPcmToCsv(activityContext, settings);
//
//                            converterPcmToCsv.Convert(recorder.getFilePath(), dateStart,DATA_VALUES_NAMES[recorder.getRecorderType()]);
//                        }
//                    });
                }
//                //Execute file scan to detect files in mtp
//                (new FileScanner()).scan(activityContext, recordFolderPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String mergedCsvPath = (new CsvMerger(csvFiles)).Merge(recordFolderPath + recordFileName);
            }
        });
        //Execute file scan to detect files in mtp
        (new FileScanner()).scan(activityContext, recordFolderPath);
        Log.d(MY_LOGS, "recorders stopped.");
    }

    private void generateOutputFile(){

    }

    @Override
    public String getFilePath() {
        return recordFullPath;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getRecorderType() {
        return MANAGER_ID;
    }

    @Override
    public void Update(int recorderId, float[] data) {

    }
}
