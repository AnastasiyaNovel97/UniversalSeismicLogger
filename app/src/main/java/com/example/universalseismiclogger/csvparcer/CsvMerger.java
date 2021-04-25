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

    public String Merge(String folderPath) {
        List<List<String[]>> readCsvList = new ArrayList<>();
        int resListLength = 1;

        FileOutputStream dataOutputStream = null;
        try {
            dataOutputStream = new FileOutputStream(folderPath + "_sum" + CSV_EXTENSION);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (CsvFile csvFile : fileList) {
            List<String[]> oneCsv = csvFile.read();
            readCsvList.add(oneCsv);
            resListLength += oneCsv.size() - 1;
        }

        List<String[]> resultCsvFile = new ArrayList<>();
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
                    currLine += readCsvList.get(i).get(addIndex[i])[j].replace("\n","") + ";";
                }
                if(firstLine){
                    addIndex[i]++;
                }
            }
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

        File checkFile = new File(folderPath + "summary" + CSV_EXTENSION);

        String mergedFilePath = "";
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

}
