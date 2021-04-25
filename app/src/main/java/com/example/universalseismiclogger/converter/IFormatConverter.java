package com.example.universalseismiclogger.converter;

import java.io.File;
import java.util.Date;

public interface IFormatConverter {

    String Convert();
    String getConvertedFilePath();
    String Convert(String filePath, Date startDate, String[] valueNames);

}
