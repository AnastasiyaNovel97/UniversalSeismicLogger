package com.example.universalseismiclogger.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.example.universalseismiclogger.R;
import com.example.universalseismiclogger.shared.DiscretizationRate;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;

public class ConfigActivity extends AppCompatActivity {

    private SharedPreferences config;
    private SharedPreferences.Editor configEditor;
    

    private Integer[] sampleRateArray = DiscretizationRate.rates;

    private Spinner spinnerSampleRate;
    private EditText editTextLogName;
    private Switch switchUseMic;
    private Switch switchUnprocessedMic;
    private Switch switchUseGyroscope;
    private Switch switchUseAccelerometer;
    private Switch switchUseCompass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        initViews();

        initSampleRateSpinner();

        loadConfig();
    }

    private void initViews(){
        spinnerSampleRate = (Spinner) findViewById(R.id.spinnerSampleRate);
        editTextLogName = (EditText) findViewById(R.id.editTextLogName);
        switchUseMic = (Switch) findViewById(R.id.switchUseMic);
        switchUnprocessedMic = (Switch) findViewById(R.id.switchUnprocessedMic);
        switchUseGyroscope = (Switch) findViewById(R.id.switchUseGyroscope);
        switchUseAccelerometer = (Switch) findViewById(R.id.switchUseAccelerometer);
        switchUseCompass = (Switch) findViewById(R.id.switchUseCompass);
    }

    private void initSampleRateSpinner() {
        // адаптер
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, sampleRateArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSampleRate = (Spinner) findViewById(R.id.spinnerSampleRate);
        spinnerSampleRate.setAdapter(adapter);
        // заголовок
        spinnerSampleRate.setPrompt("Title");
        // выделяем элемент
        spinnerSampleRate.setSelection(0);
        // устанавливаем обработчик нажатия
        spinnerSampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // показываем позицию нажатого элемента
                //Toast.makeText(getBaseContext(), "Position = " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }


    private void saveConfig(){
        configEditor = config.edit();

        configEditor.putInt(SAMPLE_RATE, (int)spinnerSampleRate.getSelectedItem());
        configEditor.putInt(SAMPLE_RATE_POSITION, spinnerSampleRate.getSelectedItemPosition());

        configEditor.putString(LOG_NAME, editTextLogName.getText().toString());

        configEditor.putBoolean(USE_MIC, switchUseMic.isChecked());
        configEditor.putBoolean(UNPROCESSED_MIC, switchUnprocessedMic.isChecked());
        configEditor.putBoolean(USE_GYROSCOPE, switchUseGyroscope.isChecked());
        configEditor.putBoolean(USE_ACCELEROMETER, switchUseAccelerometer.isChecked());
        configEditor.putBoolean(USE_COMPASS, switchUseCompass.isChecked());

        configEditor.apply();
        
    }

    private void loadConfig(){
        config = getSharedPreferences(RECORDER_CONFIG, MODE_PRIVATE);

        spinnerSampleRate.setSelection(config.getInt(SAMPLE_RATE_POSITION, 0));

        editTextLogName.setText(config.getString(LOG_NAME, "Rec"));

        switchUseMic.setChecked(config.getBoolean(USE_MIC, true));
        switchUnprocessedMic.setChecked(config.getBoolean(UNPROCESSED_MIC, false));
        switchUseGyroscope.setChecked(config.getBoolean(USE_GYROSCOPE, true));
        switchUseAccelerometer.setChecked(config.getBoolean(USE_ACCELEROMETER, true));
        switchUseCompass.setChecked(config.getBoolean(USE_COMPASS, true));

    }

    public void onBackClick(View view){
        saveConfig();
        super.onBackPressed();
    }

    @Override
    protected void onPause(){
        saveConfig();
        super.onPause();
    }

    @Override
    protected void onResume(){
        loadConfig();
        super.onResume();
    }


}