package com.example.universalseismiclogger.neuralnetwork;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class NNModelAccessor {

    private Interpreter tflite;
    private Context activityContext;

    public NNModelAccessor(Context activityContext){
        this.activityContext = activityContext;
        try {
            tflite = new Interpreter(loadModelFile());
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor=activityContext.getAssets().openFd("dnn_model.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=fileDescriptor.getStartOffset();
        long declareLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declareLength);
    }

    public float doInference(float[] input) throws IndexOutOfBoundsException{
        float[][] output=new float[1][1];
        try {
            tflite.run(input, output);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        float inferredValue=output[0][0];
        return inferredValue;
    }

}
