package com.example.universalseismiclogger.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.universalseismiclogger.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.SystemClock.*;

import com.example.universalseismiclogger.converter.ConverterPcmToCsv;
import com.example.universalseismiclogger.filescanner.FileScanner;
import com.example.universalseismiclogger.locationprovider.GpsLocationProvider;
import com.example.universalseismiclogger.permissions.PermissionRequester;
import com.example.universalseismiclogger.recorder.RecorderManager;
import com.example.universalseismiclogger.shared.ITraceable;
import com.instacart.library.truetime.TrueTime;

import static com.example.universalseismiclogger.shared.DefaultStrings.BASE_FOLDER_PATH;
import static com.example.universalseismiclogger.shared.DefaultStrings.GPS_LOCATION;
import static com.example.universalseismiclogger.shared.DefaultStrings.GPS_LOCATION_DEFAULT;
import static com.example.universalseismiclogger.shared.DefaultStrings.LOG_NAME;
import static com.example.universalseismiclogger.shared.DefaultStrings.RECORDER_CONFIG;
import static com.example.universalseismiclogger.shared.LogTags.MY_LOGS;

public class RecordingActivity extends AppCompatActivity implements ITraceable {

    private static final int CONFIG_REQUEST_CODE = 1;

    private PermissionRequester permissionRequester = new PermissionRequester();

    private boolean isRecording = false;
    GpsLocationProvider locationProvider;

    private Button buttonRec;           // Start record
    private Button buttonStop;          // Stop and save record
    private TextView textViewRec;       // Shows elapsed time of record
    private TextView textViewCurrentTime;

    private Date dateNow;

    //private RecorderMicToWav recorderMicToWav;
    private RecorderManager recorderManager = new RecorderManager();

    private SharedPreferences recorderConfig;

    private Handler customHandler = new Handler();

    private long startTime = 0L;        // start time of measuring
    private long timeInMilliseconds = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        getWindow().addFlags((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

        InitLayoutViews();

        permissionRequester.CheckPermissions(this);

        recorderConfig = getSharedPreferences(RECORDER_CONFIG, MODE_PRIVATE);

        InitRecorderManager();

        initTrueTime();

        initGpsLocationProvider();

        getGpsLocation();
    }

    private void initTrueTime(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TrueTime.build().initialize();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initGpsLocationProvider(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                locationProvider = new GpsLocationProvider(RecordingActivity.this, recorderConfig);
            }
        }).start();
    }
    public Handler mHandler;
    private void getGpsLocation(){
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (locationProvider == null){;}
                Looper.prepare();
                mHandler = new Handler(Looper.myLooper()) {
                    public void handleMessage(Message msg) {
                        int a = 0;
                    }
                };
                locationProvider.getLocation();
                Looper.loop();

                Toast.makeText(RecordingActivity.this,
                        recorderConfig.getString(GPS_LOCATION, GPS_LOCATION_DEFAULT),
                        Toast.LENGTH_LONG).show();
                Log.d(MY_LOGS, recorderConfig.getString(GPS_LOCATION, GPS_LOCATION_DEFAULT));

            }
        }).start();
    }



//    private void InitRecorderMicToWav() {
//        int sampleRate = 8000;
//        try {
//            sampleRate = (int) spinnerSampleRate.getSelectedItem();
//        }
//        catch (ClassCastException ex){
//            Log.d(TAG,"Error getting sample rate from spinner");
//        }
//
//        boolean isUnprocessed = switchUnprocessedMic.isChecked();
//        recorderMicToWav = new RecorderMicToWav(this, sampleRate, isUnprocessed);
//    }

//        private void InitRecorderConfig(){
//        SharedPreferences recorderConfig = getSharedPreferences(RECORDER_CONFIG, MODE_PRIVATE);
//        RecorderConfiguration recorderConfiguration = RecorderConfiguration.getInstance();
//        recorderConfiguration.InitConfiguration(this, recorderConfig);
//    }

    private void InitRecorderManager(){
        recorderManager.init(this, recorderConfig);
    }

    private void InitLayoutViews() {
        buttonRec = (Button) findViewById(R.id.buttonRecord);
        buttonRec.setText(R.string.record_button_start);

        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setEnabled(false);

        textViewRec = (TextView) findViewById(R.id.textViewChronometer);
        textViewCurrentTime = (TextView) findViewById(R.id.textViewCurrentTime);

        ((TextView) findViewById(R.id.textViewPath)).setText(getString(R.string.record_folder_path) + BASE_FOLDER_PATH);

        customHandler.post(updateCurrentTime);
        customHandler.post(updateCurrentLocation);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        permissionRequester.OnPermissionResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            return true;
        }
        else return super.onKeyDown(keyCode, event);
    }

    public void onConfigClick(View view){
        Intent configIntent = new Intent(this, ConfigActivity.class);
        startActivity(configIntent);
    }

    // Starts audio recording
    public void onRecordClick(View view){

        isRecording = true;

        if (TrueTime.isInitialized())
            dateNow = TrueTime.now();
        else dateNow = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH-mm-ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(dateNow);
        //EditText editTextName = (EditText) RecordingActivity.this.findViewById(R.id.editTextLogName);
        String recordFileName = recorderConfig.getString(LOG_NAME,"Rec")+ "_" + currentDateAndTime;

        buttonRec.setEnabled(false);
        buttonStop.setEnabled(true);


        startTime = uptimeMillis();
        customHandler.post(updateTimerThread);

        Toast.makeText(this, R.string.record_started, Toast.LENGTH_SHORT).show();
        recorderManager.startRecorder(recordFileName);
        //wavRecorder = new TestWavRecorder(Environment.getExternalStorageDirectory()
        // + "/UniversalSeismicLogger/"+recordFileName);
        //wavRecorder.startRecording();

    }


    //stops audio record and saves file to path
    public void onStopClick(View view){
        isRecording = false;
        startTime = 0L;
        customHandler.removeCallbacks(updateTimerThread);

        recorderManager.stopRecorder();

        buttonRec.setEnabled(true);
        buttonStop.setEnabled(false);

        Toast.makeText(this,
                recorderConfig.getString(GPS_LOCATION, GPS_LOCATION_DEFAULT),
                Toast.LENGTH_LONG).show();

        String fileSavedAtPath = getResources().getString(R.string.file_saved_at_path);
        //Toast.makeText(this, fileSavedAtPath, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, fileSavedAtPath + " " + BASE_FOLDER_PATH
                + recorderManager.getFilePath() + "/", Toast.LENGTH_LONG).show();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                ConverterPcmToCsv converterPcmToCsv = new ConverterPcmToCsv(recorderManager.getFilePath(),
                        dateNow, recorderManager.getSampleRate());
                converterPcmToCsv.Convert();

                //Execute file scan to detect files in mtp
                (new FileScanner()).scan(getBaseContext(),
                        BASE_FOLDER_PATH + recorderManager.getFilePath()+"/");
            }
        });


    }


    private final Runnable updateCurrentTime = new Runnable() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void run() {
            if(TrueTime.isInitialized()) {
                dateNow = TrueTime.now();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
                String currentDateAndTime = sdf.format(dateNow);
                textViewCurrentTime.setText(getString(R.string.current_time)+currentDateAndTime);
            }
            customHandler.postDelayed(this, 100);
        }
    };

    private final Runnable updateCurrentLocation = new Runnable() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void run() {
            String myLocation = recorderConfig.getString(GPS_LOCATION, GPS_LOCATION_DEFAULT);
            ((TextView)findViewById(R.id.textViewCurrentLocation)).setText(
                    getString(R.string.current_location)+myLocation);
            customHandler.postDelayed(this, 1000);
        }
    };

    //counting elapsed time of audio record
    private final Runnable updateTimerThread = new Runnable() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            int secs = (int) (timeInMilliseconds / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (timeInMilliseconds % 1000);
            textViewRec.setText("" + mins + ":" + String.format("%02d", secs) + ":" + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 50);
        }
    };

    @Override
    protected void onResume(){
        InitRecorderManager();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        //recorderManager.stopRecorder();
        onStopClick(new View(this));
        super.onDestroy();
    }


}
