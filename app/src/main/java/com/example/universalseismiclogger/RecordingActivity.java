package com.example.universalseismiclogger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import static android.os.SystemClock.*;

//states for easier button management
enum RecordStates{
    STOP,
    RECORDING,
    PAUSED
}

public class RecordingActivity extends AppCompatActivity {

    private Button buttonRec;           // Start/Pause record
    private Button buttonStop;          // Stop and save record
    private Button buttonReset;         // Stop and delete record
    private TextView textViewRec;       // Shows elapsed time of record

    private RecordStates currentState;  //State of Record

    private Handler customHandler = new Handler();

    private long startTime = 0L;        //start time of measuring
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;             //buffer where stored elapsed time when pause
    long updatedTime = 0L;              // elapsed time from start time + elapsed time before pause


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        currentState = RecordStates.STOP;

        buttonRec = (Button) findViewById(R.id.buttonRecord);
        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        textViewRec = (TextView) findViewById(R.id.textViewChronometer);


        buttonRec.setText(R.string.record_button_start);
    }

    // Starts audio recording and manages pause/continue
    public void onRecordClick(View view){
        if(currentState == RecordStates.STOP){ // if record does not started already

            currentState = RecordStates.RECORDING;
            startTime = uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);
            Toast.makeText(this, R.string.record_started, Toast.LENGTH_SHORT);
            buttonRec.setText(R.string.record_button_pause);
        }

        else if(currentState == RecordStates.PAUSED){ // if record was started, but paused

            currentState = RecordStates.RECORDING;
            startTime = uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);
            Toast.makeText(this, R.string.record_started, Toast.LENGTH_SHORT);
            buttonRec.setText(R.string.record_button_pause);
        }

        else if(currentState == RecordStates.RECORDING){ // if recording now

            currentState = RecordStates.PAUSED;
            timeSwapBuff += timeInMilliseconds;
            customHandler.removeCallbacks(updateTimerThread);
            Toast.makeText(this, R.string.record_paused, Toast.LENGTH_SHORT);
            buttonRec.setText(R.string.record_button_continue);
        }

    }

    //stops audio record and saves file to path
    public void onStopClick(View view){
        currentState = RecordStates.STOP;
        startTime = 0L;
        timeSwapBuff = 0L;
        customHandler.removeCallbacks(updateTimerThread);
        textViewRec.setText("0:00:000");
        Toast.makeText(this, R.string.record_stopped, Toast.LENGTH_SHORT);
        buttonRec.setText(R.string.record_button_start);
    }

    //stops record and deletes file
    public void setResetClick(View view){
        currentState = RecordStates.STOP;
        startTime = 0L;
        timeSwapBuff = 0L;
        customHandler.removeCallbacks(updateTimerThread);
        textViewRec.setText("0:00:000");
        Toast.makeText(this, R.string.record_aborted, Toast.LENGTH_SHORT);
        buttonRec.setText(R.string.record_button_start);
    }

    //counting elapsed time of audio record
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            textViewRec.setText("" + mins + ":" + String.format("%02d", secs) + ":" + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };
}
