package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.universalseismiclogger.recorder.interfaces.IRecorder;
import com.example.universalseismiclogger.recorder.interfaces.IRecorderReceiver;
import com.example.universalseismiclogger.recorder.interfaces.IRecorderTransmitter;
import com.example.universalseismiclogger.shared.ITraceable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;
import static com.example.universalseismiclogger.shared.Extensions.CSV_EXTENSION;
import static com.example.universalseismiclogger.shared.Extensions.PCM_EXTENSION;
import static com.example.universalseismiclogger.shared.LogTags.MY_LOGS;

public class RecorderOrientationSensors implements IRecorder, IRecorderTransmitter, ITraceable, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    private String filename;
    private int recorderType;
    private int recorderId;
    private int sampleRate;
    private int intervalMicroSec;
    private Context parentContext;
    private static boolean isFileSaved = false;
    private String recordFileName;
    private String recordFolderPath;
    private String recordFullPath;
    private long startDateMillis;


    private boolean isReading = false;
    private File recordFile;
    private int dataBufferSize = DATA_BUFFER_SIZE_DEFAULT*10;            // buffer size for audioRecord
    private float[] dataBuffer;
    private int bytes_per_sample = 4;
    private int samplesRead;
    private int numOfDataParams;
    private int dataSource;



    //private ObjectOutputStream dataOutputStream;
    private FileOutputStream dataOutputStream;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public RecorderOrientationSensors(int recorderType){
        this.recorderType = recorderType;
    }

    @Override
    public IRecorder init(Context activityContext, SharedPreferences settings, int id) {
        recorderId = id;
        sampleRate = settings.getInt(SAMPLE_RATE, SAMPLE_RATE_DEFAULT);
        intervalMicroSec = 1000000 / sampleRate;
        parentContext = activityContext;

        sensorManager = (SensorManager) activityContext.getSystemService(Context.SENSOR_SERVICE);
        sensor = null;
        switch (recorderType){
            case GYROSCOPE_ID:
                numOfDataParams = 3;
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                break;
            case ACCELEROMETER_ID:
                numOfDataParams = 3;
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                break;
            case COMPASS_ID:
                numOfDataParams = 1;
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                break;
        }
        dataBufferSize = DATA_BUFFER_SIZE_DEFAULT * 10 * numOfDataParams;
        return this;
    }


    private final Runnable recordingTask = new Runnable() {
        @Override
        public void run() {
            WriteDataToFile();
        }
    };

    public void WriteDataToFile(){
        isFileSaved = false;

        try {
            dataOutputStream = new FileOutputStream(recordFullPath);
            //dataOutputStream = new ObjectOutputStream(new FileOutputStream(recordFullPath));
            //dataOutputStream.flush();
            //dataOutputStream.writeFloat(numOfDataParams);
            while (isReading) {;}
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dataOutputStream.close();
                isFileSaved = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            File checkFile = new File(recordFullPath);
            Log.d(MY_LOGS, "Orientation sensors record File("+ recordFullPath +") length(): " + checkFile.length());
        }
    }


    @Override
    public void startRecorder(String fileName) throws IOException {
        this.filename = fileName;

        startDateMillis = System.currentTimeMillis();

        samplesRead = 0;

        dataOutputStream = null;

        recordFileName = fileName + "_sens"+recorderType;
        recordFolderPath = BASE_FOLDER_PATH + fileName +"/";
        recordFullPath = recordFolderPath + recordFileName + CSV_EXTENSION;
        File recordDir = new File(recordFolderPath);
        recordDir.mkdirs();
        Log.d(MY_LOGS, "sensor "+recorderType+" record start");

        samplesRead = 0;

        if (sensor != null){
            isReading = true;
            executorService.submit(recordingTask);
            sensorManager.registerListener(this, sensor, intervalMicroSec, intervalMicroSec);
        }
        else{
            throw new IOException("SensorType id="+recorderType+" is not supported by this device!");
        }

    }

    @Override
    public void stopRecorder() throws IOException {
        Log.d(MY_LOGS, "sensor "+recorderType+" record stop");
        sensorManager.unregisterListener(this);
        isReading = false;
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
        return recorderType;
    }

    private DecimalFormat df = new DecimalFormat("#####.#####");
    private boolean isFirstLine = true;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        try {
            if(dataOutputStream != null) {
                if(isFirstLine){
                    isFirstLine=false;
                    String firstLine = "time;";
                    for (int i=0; i<sensorEvent.values.length; i++)
                    {
                        firstLine+= sensorEvent.sensor.getName()+i+";";
                    }
                    firstLine+="\n";
                    dataOutputStream.write(firstLine.getBytes());
                }
                String line = String.valueOf(System.currentTimeMillis() -
                        startDateMillis);
                for (float sensorValue : sensorEvent.values) {
                    line+= "; "+df.format(sensorValue);
                }
                line+='\n';
                dataOutputStream.write(line.getBytes());
                samplesRead++;
            }
        }
        catch (ArrayIndexOutOfBoundsException | IOException aioobe){
            aioobe.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void SetReceiver(IRecorderReceiver receiver) {

    }

    @Override
    public void NotifyReceivers() {

    }
}
