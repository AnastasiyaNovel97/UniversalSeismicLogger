package com.example.universalseismiclogger.recorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.example.universalseismiclogger.recorder.interfaces.IRecorder;
import com.example.universalseismiclogger.recorder.interfaces.IRecorderReceiver;
import com.example.universalseismiclogger.shared.ITraceable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.universalseismiclogger.shared.DefaultStrings.*;
import static com.example.universalseismiclogger.shared.Extensions.*;
import static com.example.universalseismiclogger.shared.LogTags.MY_LOGS;

public class RecorderMic implements IRecorder, ITraceable {

    private Context parentContext;
    private volatile boolean isFileSaved = false;
    private String recordFileName;
    private String recordFilePath;
    private String recordFullPath;
    private volatile boolean isReading = false;
    private File recordFile;
    private int samplesRead;
    private int recorderId;
    private int myBufferSize = DATA_BUFFER_SIZE_DEFAULT;            // buffer size for audioRecord
    private int audioSource = MediaRecorder.AudioSource.DEFAULT;
    private AudioRecord audioRecord;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private FileOutputStream fOutputStream = null;

    private IRecorderReceiver recorderReceiver;

    private final Runnable recordingTask = new Runnable() {
        @Override
        public void run() {
            writeAudioDataToFile();
        }
    };

    private void writeAudioDataToFile() {
        byte data[] = new byte[myBufferSize];
        isFileSaved = false;
        fOutputStream = null;
        try {
            fOutputStream = new FileOutputStream(recordFilePath + recordFileName + PCM_EXTENSION);
            try {
                //os.write(ByteBuffer.allocate(4).putInt(1).array());
                int read = 0;
                if (null != fOutputStream) {
                    fOutputStream.write(data);
                    while (isReading) {
                        read = audioRecord.read(data, 0, myBufferSize);
                        if (read > 0) {
                            samplesRead+= read;
                        }

                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                fOutputStream.write(data);
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
                fOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            isFileSaved = true;
            File checkFile = new File(recordFilePath + recordFileName + PCM_EXTENSION);
            Log.d(MY_LOGS, "writeAudioDataToFile record File("+ recordFilePath + recordFileName
                    + PCM_EXTENSION +") length(): " + checkFile.length());
        }
    }

    private void stopWriteAudioDataToFile(){
        try {
            fOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        isFileSaved = true;
        File checkFile = new File(recordFilePath + recordFileName + PCM_EXTENSION);
        Log.d(MY_LOGS, "writeAudioDataToFile record File("+ recordFilePath + recordFileName
                + PCM_EXTENSION +") length(): " + checkFile.length());
    }

    public RecorderMic(){};

    public RecorderMic(Context activityContext, int sampleRate){
        this(activityContext, sampleRate, false);
    }

    public RecorderMic(Context activityContext, int sampleRate, boolean isUnprocessed){
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

    public RecorderMic(Context activityContext, int sampleRate, int channelConfig,
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
    public IRecorder init(Context activityContext, SharedPreferences settings, int id) {
        recorderId = id;
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
        samplesRead = 0;
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
            isFileSaved = false;
            //executorService.submit(recordingTask);
            executorService.execute(recordingTask);
        }
        else {
            trace("AudioRecord is not initialized!!!");
        }
    }

    @Override
    public void stopRecorder() {
        Log.d(MY_LOGS, "mic record stop");
        //audioRecord.stop();
        isReading = false;

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stopWriteAudioDataToFile();

        boolean isfs = false;
        while(this.isFileSaved == false) {
            isfs = isFileSaved;
        }
//        while(!isFileSaved) {
//
//        }

        if (null != audioRecord) {
            isReading = false;

            if (audioRecord.getState() == 1)
                audioRecord.stop();
            audioRecord.release();
        }

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
