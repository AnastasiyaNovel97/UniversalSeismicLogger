package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.example.universalseismiclogger.converter.ConverterMicPcmToCsv;
import com.example.universalseismiclogger.filescanner.FileScanner;
import com.example.universalseismiclogger.shared.ITraceable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;
import static com.example.universalseismiclogger.shared.Extensions.*;
import static com.example.universalseismiclogger.shared.LogTags.MY_LOGS;

public class RecorderMicToWav implements IRecorder, ITraceable {

    private Context parentContext;
    private static boolean isFileSaved = false;
    private String recordFileName;
    private String recordFilePath;
    private String recordFullPath;
    private boolean isReading = false;
    private File recordFile;
    private int myBufferSize = DATA_BUFFER_SIZE_DEFAULT;            // buffer size for audioRecord
    private int audioSource = MediaRecorder.AudioSource.DEFAULT;
    private AudioRecord audioRecord;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Runnable recordingTask = new Runnable() {
        @Override
        public void run() {
            writeAudioDataToFile();
        }
    };

    private void writeAudioDataToFile() {
        byte data[] = new byte[myBufferSize];
        isFileSaved = false;
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(recordFilePath + recordFileName + PCM_EXTENSION);
            try {
                os.write(ByteBuffer.allocate(4).putInt(1).array());
                int read = 0;
                if (null != os) {
                    os.write(data);
                    while (isReading) {
                        read = audioRecord.read(data, 0, myBufferSize);
                        if (read > 0) {
                        }

                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                os.close();
                isFileSaved = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            File checkFile = new File(recordFilePath + recordFileName + PCM_EXTENSION);
            Log.d(MY_LOGS, "writeAudioDataToFile record File("+ recordFilePath + recordFileName
                    + PCM_EXTENSION +") length(): " + checkFile.length());
        }
    }

    public RecorderMicToWav(){};

    public RecorderMicToWav(Context activityContext, int sampleRate){
        this(activityContext, sampleRate, false);
    }

    public RecorderMicToWav(Context activityContext, int sampleRate, boolean isUnprocessed){
        parentContext = activityContext;
        if (isUnprocessed) {audioSource = MediaRecorder.AudioSource.UNPROCESSED;}
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize * 4;

        Log.d(MY_LOGS, "minInternalBufferSize = " + minInternalBufferSize
                + ", internalBufferSize = " + internalBufferSize
                //+ ", myBufferSize = " + myBufferSize
                + ", SampleRate = " + sampleRate);

        AudioRecordInitialization(sampleRate, channelConfig, audioFormat, internalBufferSize);
    }

    public RecorderMicToWav(Context activityContext, int sampleRate, int channelConfig,
                            int audioFormat, int internalBufferSize) {
        parentContext = activityContext;
        AudioRecordInitialization(sampleRate, channelConfig, audioFormat, internalBufferSize);
    }

    private void AudioRecordInitialization(int sampleRate, int channelConfig, int audioFormat,
                                           int internalBufferSize){
        audioRecord = new AudioRecord(audioSource,
                sampleRate, channelConfig, audioFormat, internalBufferSize);
    }

    @Override
    public IRecorder init(Context activityContext, SharedPreferences settings) {
        int sampleRate = settings.getInt(SAMPLE_RATE, 4000);
        boolean unproc = settings.getBoolean(UNPROCESSED_MIC, false);
        parentContext = activityContext;
        if (unproc) audioSource = MediaRecorder.AudioSource.UNPROCESSED;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize * 4;

        Log.d(MY_LOGS, "minInternalBufferSize = " + minInternalBufferSize
                + ", internalBufferSize = " + internalBufferSize
                //+ ", myBufferSize = " + myBufferSize
                + ", SampleRate = " + sampleRate);

        AudioRecordInitialization(sampleRate, channelConfig, audioFormat, internalBufferSize);
        return this;
    }

    @Override
    public void startRecorder(String fileName) {
        recordFileName = fileName + "_mic";
        recordFilePath = BASE_FOLDER_PATH + fileName +"/";
        recordFullPath = recordFilePath + recordFileName + PCM_EXTENSION;
        File recordDir = new File(recordFilePath);
        recordDir.mkdirs();
        Log.d(MY_LOGS, "mic record start");
        if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            int recordingState = audioRecord.getRecordingState();
            Log.d(MY_LOGS, "recordingState = " + recordingState);
            isReading = true;
            executorService.submit(recordingTask);
        }
        else {
            trace("AudioRecord is not initialized!!!");
        }
    }

    @Override
    public void stopRecorder() {
        Log.d(MY_LOGS, "mic record stop");
        audioRecord.stop();

        if (null != audioRecord) {
            isReading = false;

            if (audioRecord.getState() == 1)
                audioRecord.stop();
            //audioRecord.release();
        }

        while(!isFileSaved) { }

        (new WavGenerator(audioRecord, myBufferSize))
                .copyPcmToWav(recordFilePath + recordFileName + PCM_EXTENSION,
                        recordFilePath + recordFileName + WAV_EXTENSION);


    }

    @Override
    public String getFilePath() {return recordFullPath;}

    @Override
    public int getSampleRate() {
        return audioRecord.getSampleRate();
    }

    @Override
    public int getRecorderType() {
        return MICROPHONE_ID;
    }


}
