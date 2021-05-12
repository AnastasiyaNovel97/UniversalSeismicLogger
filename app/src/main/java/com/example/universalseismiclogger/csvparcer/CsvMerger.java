package com.example.universalseismiclogger.csvparcer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static com.example.universalseismiclogger.shared.Extensions.CSV_EXTENSION;

public class CsvMerger {

    private Vector<CsvFile> fileList;

    public CsvMerger(Vector<CsvFile> fileList){
        this.fileList = fileList;
    }

    public List<String[]> Merge(String folderPath) {
        String mergedFilePath = folderPath + "_sum" + CSV_EXTENSION;
        List<List<String[]>> readCsvList = new ArrayList<>();
        int resListLength = 1;

        FileOutputStream dataOutputStream = null;
        try {
            dataOutputStream = new FileOutputStream(mergedFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (CsvFile csvFile : fileList) {
            List<String[]> oneCsv = csvFile.read();
            readCsvList.add(oneCsv);
            resListLength += oneCsv.size() - 1;
        }

        List<String[]> resultCsvFile = new ArrayList<>();
        int resultLineIndex=0;
        int[] addIndex = new int[readCsvList.size()];

        boolean firstLine = true;
        for (String[] csvLine :
                readCsvList.get(0)) {
            String currLine = "";
            for (String csvLineString :
                    csvLine) {
                currLine += csvLineString.replace("\n","") + ";";
            }

            Long currTime = ParseLong(csvLine[0]);

            for (int i = 1; i < readCsvList.size(); i++) {
                if(!firstLine){
                    Long additionalTime = ParseLong(readCsvList.get(i).get(addIndex[i])[0]);

                    try {
                        while (currTime > additionalTime) {
                            addIndex[i]++;
                            additionalTime = ParseLong(readCsvList.get(i).get(addIndex[i])[0]);
                        }
                    } catch (Exception e) {
                        addIndex[i]--;
                    }
                }


                for (int j = 1; j < readCsvList.get(i).get(addIndex[i]).length; j++) {
                    if(j!=1 || i!=1) currLine += ";";
                    currLine += readCsvList.get(i).get(addIndex[i])[j].replace("\n","");
                }
                if(firstLine){
                    addIndex[i]++;
                }
            }
            resultCsvFile.add(currLine.split(";"));
            currLine += "\n";
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.write(currLine.getBytes());
                }
            } catch (ArrayIndexOutOfBoundsException | IOException aioobe) {
                aioobe.printStackTrace();
            }
            firstLine=false;
        }


        try {
            dataOutputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return resultCsvFile;

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

}
