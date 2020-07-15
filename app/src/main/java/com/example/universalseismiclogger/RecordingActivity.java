package com.example.universalseismiclogger;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import static android.os.SystemClock.*;

//states for easier button management
enum RecordStates{
    STOP,
    RECORDING,
    PAUSED
}


public class RecordingActivity extends AppCompatActivity {

    final String TAG = "myLogs";
    private final static int MY_PERMISSIONS_REQUEST_RECORD_AUDIO=0;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE=1;

    private final static String FILE_FOLDER_PATH = Environment.getExternalStorageDirectory() + "/UniversalSeismicLogger/";

    private Button buttonRec;           // Start/Pause record
    private Button buttonStop;          // Stop and save record
    private Button buttonReset;         // Stop and delete record
    private TextView textViewRec;       // Shows elapsed time of record
    private Spinner spinnerSampleRate;
    private String recordFileName;


    Integer[] sampleRateArray = {1000 , 2000, 4000, 8000, 11025, 16000, 22050, 32000, 44100, 48000};
    long totalFramesCount;

    private RecordStates currentState;  // State of Record

    private Handler customHandler = new Handler();

    private long startTime = 0L;        // start time of measuring
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;             // variable where stored elapsed time when pause
    long updatedTime = 0L;              // elapsed time from start time + elapsed time before pause


    int myBufferSize = 8192;            // buffer size for audioRecord
    AudioRecord audioRecord;
    boolean isReading = false;


    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };



    boolean waitForPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        currentState = RecordStates.STOP; // set default state of recorder

        // initialize global views
        buttonRec = (Button) findViewById(R.id.buttonRecord);
        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        textViewRec = (TextView) findViewById(R.id.textViewChronometer);

        buttonRec.setText(R.string.record_button_start);

        // адаптер
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, sampleRateArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSampleRate = (Spinner) findViewById(R.id.spinnerSampleRate);
        spinnerSampleRate.setAdapter(adapter);
        // заголовок
        spinnerSampleRate.setPrompt("Title");
        // выделяем элемент
        spinnerSampleRate.setSelection(3);
        // устанавливаем обработчик нажатия
        spinnerSampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // показываем позицию нажатого элемента
                Toast.makeText(getBaseContext(), "Position = " + position, Toast.LENGTH_SHORT).show();
                createAudioRecorder();
                Log.d(TAG, "init state = " + audioRecord.getState());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //spinnerSampleRate.getSelectedItem();

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
//                PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
//                //Do something
//            } else {
//                waitForPermission = true;
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
//            }
//        } else waitForPermission = false;
//
//
//        while (waitForPermission) {};
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
//                PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                //Do something
//            } else {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
//            }
//        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    showSingleButtonAlertDialog("Внимание","Без разрешения на запись аудио работа программы невозможна.","ОК");

                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    showSingleButtonAlertDialog("Внимание","Без разрешения на создание файлов работа программы невозможна.","ОК");

                }
                break;
            }
        }
        waitForPermission = false;
    }


    public void showSingleButtonAlertDialog(String title, String message, String okBtnCaption){
        AlertDialog.Builder builder = new AlertDialog.Builder(RecordingActivity.this);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(okBtnCaption,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    // Starts audio recording and manages pause/continue
    public void onRecordClick(View view){
        if(currentState == RecordStates.STOP){ // if record does not started already
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());
            EditText editTextName = (EditText) RecordingActivity.this.findViewById(R.id.editTextLogName);
            recordFileName = editTextName.getText() + currentDateandTime + ".log.txt";
            currentState = RecordStates.RECORDING;
            startTime = uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);
            Toast.makeText(this, R.string.record_started, Toast.LENGTH_SHORT);
            buttonRec.setText(R.string.record_button_pause);
            recordStart(new View(this));
        }

        else if(currentState == RecordStates.PAUSED){ // if record was started, but paused

            currentState = RecordStates.RECORDING;
            startTime = uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);
            Toast.makeText(this, R.string.record_started, Toast.LENGTH_SHORT);
            buttonRec.setText(R.string.record_button_pause);
            recordStart(new View(this));
        }

        else if(currentState == RecordStates.RECORDING){ // if recording now

            currentState = RecordStates.PAUSED;
            timeSwapBuff += timeInMilliseconds;
            customHandler.removeCallbacks(updateTimerThread);
            Toast.makeText(this, R.string.record_paused, Toast.LENGTH_SHORT);
            buttonRec.setText(R.string.record_button_continue);
            recordStop(new View(this));
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
        recordStop(new View(this));
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
        recordStop(new View(this));

        File file = new File(FILE_FOLDER_PATH + recordFileName);
        boolean deleted = file.delete();
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

    // create instance of AudioRecorder class
    void createAudioRecorder() {
        int sampleRate = 8000;

        try {
            sampleRate = (int) spinnerSampleRate.getSelectedItem();
        }
        catch (ClassCastException ex){
            Log.d(TAG,"Error getting sample rate from spinner");
        }

        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize * 4;
        Log.d(TAG, "minInternalBufferSize = " + minInternalBufferSize
                + ", internalBufferSize = " + internalBufferSize
                + ", myBufferSize = " + myBufferSize
                + ", Samplerate = " + sampleRate);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, internalBufferSize);


    }

    // Start recording audio
    public void recordStart(View v) {

        Log.d(TAG, "record start");
        audioRecord.startRecording();
        int recordingState = audioRecord.getRecordingState();
        Log.d(TAG, "recordingState = " + recordingState);
        readStart(v);
    }

    // Stop recording audio
    public void recordStop(View v) {
        Log.d(TAG, "record stop");
        readStop(v);
        audioRecord.stop();

    }

    int readSampleCount = 0;
    public void readStart(View v) {
        Log.d(TAG, "read start");

        Log.d(TAG,"starting to write audio into file");
        isReading = true;
        final File file = new File(Environment.getExternalStorageDirectory() + "/UniversalSeismicLogger/");

        boolean dirCreated = file.mkdir();
        final Vector<Short> fileVector = new Vector<Short>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = FILE_FOLDER_PATH + recordFileName;
                short[] sData = new short[myBufferSize];

                while (isReading) {
                    // gets the voice output from microphone to byte format

                    readSampleCount += audioRecord.read(sData, 0, myBufferSize);
                    Log.d(TAG, "data read " + readSampleCount+"("+myBufferSize+")");
                    for (short sample:
                         sData) {
                        fileVector.add(sample);
                    }
                }

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(filePath,true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    Log.d(TAG, "saving file");
                    String outputStr = "";
                    Vector<Byte> byteVector = new Vector<Byte>();
                    byte[] vOut;
                    for (short sample: fileVector) {
                        vOut = (sample + "\n").getBytes();
                        os.write(vOut, 0, vOut.length);
                    }

                    os.close();
                    MediaScannerConnection.scanFile(getBaseContext(), new String[] { filePath }, null, null);

                    Log.d(TAG,filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }



    public void readStop(View v) {
        Log.d(TAG, "read stop");
        isReading = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isReading = false;
        if (audioRecord != null) {
            audioRecord.release();
        }
    }



































    private static class RecordWaveTask extends AsyncTask<File, Void, Object[]> {

        // Configure me!
        private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
        private static final int SAMPLE_RATE = 44100; // Hz
        private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
        //

        private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);

        private Context ctx;

        private RecordWaveTask(Context ctx) {
            setContext(ctx);
        }

        private void setContext(Context ctx) {
            this.ctx = ctx;
        }

        /**
         * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
         * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
         * the WAV header to include the proper final chunk sizes.
         *
         * @param files Index 0 should be the file to write to
         * @return Either an Exception (error) or two longs, the filesize, elapsed time in ms (success)
         */
        @Override
        protected Object[] doInBackground(File... files) {
            AudioRecord audioRecord = null;
            FileOutputStream wavOut = null;
            long startTime = 0;
            long endTime = 0;

            try {
                // Open our two resources
                audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
                wavOut = new FileOutputStream(files[0]);

                // Write out the wav file header
                writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

                // Avoiding loop allocations
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean run = true;
                int read;
                long total = 0;

                // Let's go
                startTime = SystemClock.elapsedRealtime();
                audioRecord.startRecording();
                while (run && !isCancelled()) {
                    read = audioRecord.read(buffer, 0, buffer.length);

                    // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                    if (total + read > 4294967295L) {
                        // Write as many bytes as we can before hitting the max size
                        for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
                            wavOut.write(buffer[i]);
                        }
                        run = false;
                    } else {
                        // Write out the entire read buffer
                        wavOut.write(buffer, 0, read);
                        total += read;
                    }
                }
            } catch (IOException ex) {
                return new Object[]{ex};
            } finally {
                if (audioRecord != null) {
                    try {
                        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord.stop();
                            endTime = SystemClock.elapsedRealtime();
                        }
                    } catch (IllegalStateException ex) {
                        //
                    }
                    if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        audioRecord.release();
                    }
                }
                if (wavOut != null) {
                    try {
                        wavOut.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }

            try {
                // This is not put in the try/catch/finally above since it needs to run
                // after we close the FileOutputStream
                updateWavHeader(files[0]);
            } catch (IOException ex) {
                return new Object[] { ex };
            }

            return new Object[] { files[0].length(), endTime - startTime };
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out         The stream to write the header to
         * @param channelMask An AudioFormat.CHANNEL_* mask
         * @param sampleRate  The sample rate in hertz
         * @param encoding    An AudioFormat.ENCODING_PCM_* value
         * @throws IOException
         */
        private static void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
            short channels;
            switch (channelMask) {
                case AudioFormat.CHANNEL_IN_MONO:
                    channels = 1;
                    break;
                case AudioFormat.CHANNEL_IN_STEREO:
                    channels = 2;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable channel mask");
            }

            short bitDepth;
            switch (encoding) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    bitDepth = 8;
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    bitDepth = 16;
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    bitDepth = 32;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable encoding");
            }

            writeWavHeader(out, channels, sampleRate, bitDepth);
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out        The stream to write the header to
         * @param channels   The number of channels
         * @param sampleRate The sample rate in hertz
         * @param bitDepth   The bit depth
         * @throws IOException
         */
        private static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
            // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
            byte[] littleBytes = ByteBuffer
                    .allocate(14)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(channels)
                    .putInt(sampleRate)
                    .putInt(sampleRate * channels * (bitDepth / 8))
                    .putShort((short) (channels * (bitDepth / 8)))
                    .putShort(bitDepth)
                    .array();

            // Not necessarily the best, but it's very easy to visualize this way
            out.write(new byte[]{
                    // RIFF header
                    'R', 'I', 'F', 'F', // ChunkID
                    0, 0, 0, 0, // ChunkSize (must be updated later)
                    'W', 'A', 'V', 'E', // Format
                    // fmt subchunk
                    'f', 'm', 't', ' ', // Subchunk1ID
                    16, 0, 0, 0, // Subchunk1Size
                    1, 0, // AudioFormat
                    littleBytes[0], littleBytes[1], // NumChannels
                    littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                    littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                    littleBytes[10], littleBytes[11], // BlockAlign
                    littleBytes[12], littleBytes[13], // BitsPerSample
                    // data subchunk
                    'd', 'a', 't', 'a', // Subchunk2ID
                    0, 0, 0, 0, // Subchunk2Size (must be updated later)
            });
        }

        /**
         * Updates the given wav file's header to include the final chunk sizes
         *
         * @param wav The wav file to update
         * @throws IOException
         */
        private static void updateWavHeader(File wav) throws IOException {
            byte[] sizes = ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    // There are probably a bunch of different/better ways to calculate
                    // these two given your circumstances. Cast should be safe since if the WAV is
                    // > 4 GB we've already made a terrible mistake.
                    .putInt((int) (wav.length() - 8)) // ChunkSize
                    .putInt((int) (wav.length() - 44)) // Subchunk2Size
                    .array();

            RandomAccessFile accessWave = null;
            //noinspection CaughtExceptionImmediatelyRethrown
            try {
                accessWave = new RandomAccessFile(wav, "rw");
                // ChunkSize
                accessWave.seek(4);
                accessWave.write(sizes, 0, 4);

                // Subchunk2Size
                accessWave.seek(40);
                accessWave.write(sizes, 4, 4);
            } catch (IOException ex) {
                // Rethrow but we still close accessWave in our finally
                throw ex;
            } finally {
                if (accessWave != null) {
                    try {
                        accessWave.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }
        }

        @Override
        protected void onCancelled(Object[] results) {
            // Handling cancellations and successful runs in the same way
            onPostExecute(results);
        }

        @Override
        protected void onPostExecute(Object[] results) {
            Throwable throwable = null;
            if (results[0] instanceof Throwable) {
                // Error
                throwable = (Throwable) results[0];
                Log.e(RecordWaveTask.class.getSimpleName(), throwable.getMessage(), throwable);
            }

            // If we're attached to an activity
            if (ctx != null) {
                if (throwable == null) {
                    // Display final recording stats
                    double size = (long) results[0] / 1000000.00;
                    long time = (long) results[1] / 1000;
                    Toast.makeText(ctx, String.format(Locale.getDefault(), "%.2f MB / %d seconds",
                            size, time), Toast.LENGTH_LONG).show();
                } else {
                    // Error
                    Toast.makeText(ctx, throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
