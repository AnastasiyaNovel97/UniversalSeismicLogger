package com.example.universalseismiclogger.converter;

import android.content.Context;

import com.example.universalseismiclogger.csvparcer.CsvFile;
import com.example.universalseismiclogger.neuralnetwork.NNModelAccessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static com.example.universalseismiclogger.shared.Extensions.CSV_EXTENSION;

public class ConverterSensorsToSeismic {

    private List<String[]> oneCsv;
    private NNModelAccessor dnnModel;
    private Context activityContext;

    public ConverterSensorsToSeismic(List<String[]> oneCsv, Context activityContext){
        this.activityContext = activityContext;
        dnnModel = new NNModelAccessor(activityContext);
        this.oneCsv = oneCsv;
    }

    public String Convert(String folderPath, String fileName) throws FileNotFoundException {

        String mergedFilePath = folderPath+ fileName + "_neuron" + CSV_EXTENSION;

        FileOutputStream dataOutputStream = null;
        try {
            dataOutputStream = new FileOutputStream(mergedFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<String[]> resultCsvFile = new ArrayList<>();

        String headerLine = oneCsv.get(0)[0] + oneCsv.get(0)[1]+'\n';
        try {
            if (dataOutputStream != null) {
                dataOutputStream.write(headerLine.getBytes());
            }
        } catch (ArrayIndexOutOfBoundsException | IOException aioobe) {
            aioobe.printStackTrace();
        }

        float[] modelParams = new float[10];

        for(int i=1; i< oneCsv.size(); i++){
            modelParams[0] = GetParam(i,0);
            modelParams[1] = GetParam(i,2);
            modelParams[2] = GetParam(i,3);
            modelParams[3] = GetParam(i,4);
            modelParams[4] = GetParam(i,5);
            modelParams[5] = GetParam(i,6);
            modelParams[6] = GetParam(i,7);
            modelParams[7] = GetParam(i,8);
            modelParams[8] = GetParam(i,9);
            modelParams[9] = GetParam(i,10);

            float convertedData = dnnModel.doInference(modelParams);
            String convertedLine = GetParam(i,0) +";"+convertedData+'\n';
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.write(convertedLine.getBytes());
                }
            } catch (ArrayIndexOutOfBoundsException | IOException aioobe) {
                aioobe.printStackTrace();
            }
        }

        try {
            dataOutputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return mergedFilePath;

    }

    private long ParseLong(String strNum){
        long myNum = 0;

        try {
            myNum = Integer.parseInt(strNum);
        } catch(NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return myNum;
    }

    private float ParseFloat(String strNum){
        float myNum = 0;

        try {
            myNum = Float.parseFloat(strNum);
        } catch(NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return myNum;
    }

    private float GetParam(int i, int j){
        float result = 0;
        try{
            result = ParseFloat(oneCsv.get(i)[j]);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

}
