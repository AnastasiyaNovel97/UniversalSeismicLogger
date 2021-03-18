package com.example.universalseismiclogger.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.universalseismiclogger.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Intent myIntent = new Intent(MainActivity.this, RecordingActivity.class);
//        startActivity(myIntent);
    }

    public void onClick(View view){
        setContentView(R.layout.activity_main);
        Intent myIntent = new Intent(MainActivity.this, RecordingActivity.class);
        startActivity(myIntent);
    }
}
