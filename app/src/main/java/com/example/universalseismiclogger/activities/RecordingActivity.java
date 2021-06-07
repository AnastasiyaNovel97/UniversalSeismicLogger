package com.example.universalseismiclogger.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.universalseismiclogger.InfoActivity;
import com.example.universalseismiclogger.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.SystemClock.*;

import com.example.universalseismiclogger.converter.ConverterMicPcmToCsv;
import com.example.universalseismiclogger.filescanner.FileScanner;
import com.example.universalseismiclogger.locationprovider.GpsLocationProvider;
import com.example.universalseismiclogger.permissions.PermissionRequester;
import com.example.universalseismiclogger.recorder.RecorderManager;
import com.example.universalseismiclogger.recorder.RecorderValue;
import com.example.universalseismiclogger.shared.ITraceable;
import com.google.android.material.navigation.NavigationView;
import com.instacart.library.truetime.TrueTime;
import com.mxn.soul.flowingdrawer_core.ElasticDrawer;
import com.mxn.soul.flowingdrawer_core.FlowingDrawer;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;
import static com.example.universalseismiclogger.shared.LogTags.MY_LOGS;

public class RecordingActivity extends AppCompatActivity implements ITraceable {

    private AppBarConfiguration mAppBarConfiguration;

    private static final int CONFIG_REQUEST_CODE = 1;

    private PermissionRequester permissionRequester = new PermissionRequester();

    private boolean isRecording = false;
    GpsLocationProvider locationProvider;

    private Button buttonRec;           // Start record
    private Button buttonStop;          // Stop and save record
    private Button buttonConfig;
    private TextView textViewRec;       // Shows elapsed time of record
    private TextView textViewCurrentTime;

    private Date dateNow;

    RealtimeUpdates realtimeUpdates;

    //private RecorderMicToWav recorderMicToWav;
    private RecorderManager recorderManager = new RecorderManager();

    private SharedPreferences recorderConfig;

    private Handler customHandler = new Handler();

    private long startTime = 0L;        // start time of measuring
    private long timeInMilliseconds = 0L;


    private FlowingDrawer mDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_recording);

        InitLayoutAndDrawer();

        getWindow().addFlags((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

        InitLayoutViews();

        permissionRequester.CheckPermissions(this);

        recorderConfig = getSharedPreferences(RECORDER_CONFIG, MODE_PRIVATE);

        //InitRecorderManager();

        initTrueTime();

        initGpsLocationProvider();

        getGpsLocation();

        initRecorderManager();

        realtimeUpdates = (RealtimeUpdates) getSupportFragmentManager().findFragmentById(R.id.fragmentGraph);
        realtimeUpdates.generateData();

        File folder = new File(BASE_FOLDER_PATH);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }

    }

    private void InitLayoutAndDrawer(){

        setContentView(R.layout.activity_test_nav);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = (FlowingDrawer) findViewById(R.id.drawerlayout);
        mDrawer.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL);
        mDrawer.setOnDrawerStateChangeListener(new ElasticDrawer.OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                if (newState == ElasticDrawer.STATE_CLOSED) {
                    Log.i("MainActivity", "Drawer STATE_CLOSED");
                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
                Log.i("MainActivity", "openRatio=" + openRatio + " ,offsetPixels=" + offsetPixels);
            }
        });
        getSupportActionBar().setHomeButtonEnabled(true);

//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawerlayout);
//        NavigationView navigationView = findViewById(R.id.nav_view);
////         Passing each menu ID as a set of Ids because each
////         menu should be considered as top level destinations.
//        getSupportActionBar().setHomeButtonEnabled(true);
//        mAppBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
//                .setDrawerLayout(drawer)
//                .build();
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
//        NavigationUI.setupWithNavController(navigationView, navController);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.test_nav, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void initRecorderManager() {
        recorderManager = (RecorderManager) new RecorderManager().init(this, recorderConfig);
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


//    private void InitRecorderManager(){
//        recorderManager.init(this, recorderConfig);
//    }

    private void InitLayoutViews() {
        buttonRec = (Button) findViewById(R.id.buttonRecord);
        buttonRec.setText(R.string.record_button_start);

        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setEnabled(false);
        buttonStop.setVisibility(View.INVISIBLE);

        buttonConfig = (Button) findViewById(R.id.button_config);


        textViewRec = (TextView) findViewById(R.id.textViewChronometer);
        textViewCurrentTime = (TextView) findViewById(R.id.textViewCurrentTime);

        ((TextView) findViewById(R.id.textViewPath)).setText(getString(R.string.record_folder_path) + BASE_FOLDER_PATH);

        //customHandler.post(updateCurrentTime);
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

    public void onMenuConfigClick(MenuItem item){
        //onConfigClick(new View(this));
        Intent configIntent = new Intent(this, ConfigActivity.class);
        startActivity(configIntent);
    }

    // Starts audio recording
    public void onRecordClick(View view){



        isRecording = true;


        if (TrueTime.isInitialized())
            dateNow = TrueTime.now();
        else dateNow = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yy_MM_dd_HH-mm-ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(dateNow);
        //EditText editTextName = (EditText) RecordingActivity.this.findViewById(R.id.editTextLogName);
        String recordFileName = recorderConfig.getString(LOG_NAME, LOG_NAME_DEFAULT)+ "_" + currentDateAndTime;

        buttonRec.setVisibility(View.INVISIBLE);
        buttonRec.setEnabled(false);

        buttonConfig.setVisibility(View.INVISIBLE);
        buttonConfig.setEnabled(false);

        buttonStop.setVisibility(View.VISIBLE);
        buttonStop.setEnabled(true);


        startTime = uptimeMillis();
        customHandler.post(updateTimerThread);

        Toast.makeText(this, R.string.record_started, Toast.LENGTH_SHORT).show();
        initRecorderManager();
        recorderManager.SetDate(dateNow);
        recorderManager.startRecorder(recordFileName);
        realtimeUpdates.StartShow();
        //wavRecorder = new TestWavRecorder(Environment.getExternalStorageDirectory()
        // + "/UniversalSeismicLogger/"+recordFileName);
        //wavRecorder.startRecording();

    }


    //stops audio record and saves file to path
    public void onStopClick(View view){
        isRecording = false;
        startTime = 0L;
        customHandler.removeCallbacks(updateTimerThread);
        textViewRec.setText("0:00:000");


        recorderManager.stopRecorder();

        buttonRec.setEnabled(true);
        buttonRec.setVisibility(View.VISIBLE);

        buttonConfig.setEnabled(true);
        buttonConfig.setVisibility(View.VISIBLE);

        buttonStop.setEnabled(false);
        buttonStop.setVisibility(View.INVISIBLE);

        Toast.makeText(this,
                recorderConfig.getString(GPS_LOCATION, GPS_LOCATION_DEFAULT),
                Toast.LENGTH_LONG).show();

        String fileSavedAtPath = getResources().getString(R.string.file_saved_at_path);
        //Toast.makeText(this, fileSavedAtPath, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, fileSavedAtPath + " " + BASE_FOLDER_PATH
                + recorderManager.getFilePath() + "/", Toast.LENGTH_LONG).show();
        realtimeUpdates.StopShow();


    }

    public void onMenuButtonClick(MenuItem v){

        if(v.getItemId() == R.id.nav_recording){
            Toast.makeText(this,"You are already here!",Toast.LENGTH_LONG).show();
        }

        else if(v.getItemId() == R.id.nav_settings){
            onConfigClick(new View(this));
        }

        else if(v.getItemId() == R.id.nav_folder){
            open_file_button(new View(this));
        }

        else if(v.getItemId() == R.id.nav_information){
            startActivity(new Intent(this, InfoActivity.class));
        }

        else if(v.getItemId() == R.id.nav_share){
            Toast.makeText(this,"Not implemented yet",Toast.LENGTH_SHORT).show();
        }

        else if(v.getItemId() == R.id.nav_rate){
            Toast.makeText(this,"Not implemented yet",Toast.LENGTH_SHORT).show();
        }

        else {
            Toast.makeText(this,"Wrong button!",Toast.LENGTH_SHORT).show();
        }
    }


    private final Runnable updateCurrentTime = new Runnable() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void run() {
            if(TrueTime.isInitialized()) {
                dateNow = TrueTime.now();
            }
            else dateNow = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            String currentDateAndTime = sdf.format(dateNow);
            textViewCurrentTime.setText(getString(R.string.current_time)+currentDateAndTime);

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

//    float loudMin = 0;
//    float loudMax= 10f;
//    private final Runnable updateLoudBar = new Runnable() {
//        public void run() {
//            float value = recorderValue.GetValue();
//
////            if(value > loudMax){
////                loudMax = value;
////            }
////            else if(value < loudMin){
////                loudMin = value;
////            }
//
//            int progress = (int)(value/loudMax)*100;
//
//            progressBar.setProgress(progress);
//            customHandler.postDelayed(this, 50);
//        }
//    };

    @Override
    protected void onPause(){
        if(isRecording){
            onStopClick(new View(this));
        }
        super.onPause();
    }

    @Override
    protected void onResume(){
        initRecorderManager();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        //recorderManager.stopRecorder();
        onStopClick(new View(this));
        super.onDestroy();
    }

    public void open_file_button(View v){
        Uri folder = Uri.parse(BASE_FOLDER_PATH);
        Intent myIntent = new Intent(Intent.ACTION_GET_CONTENT);
        myIntent.setType("*/*");
        startActivityForResult(myIntent,228);

        //startActivity(new Intent(this, TestNavActivity.class));

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onSectionAttached(){

    }

}
