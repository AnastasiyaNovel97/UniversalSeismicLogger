package com.example.universalseismiclogger.filescanner;

import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.File;
import java.io.FileFilter;

public class FileScanner {

    public void scan(Context baseContext, String folderPath) {
        // Scan files only (not folders);
        File[] files = new File(folderPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        String[] paths = new String[files.length];
        for (int co=0; co< files.length; co++)
            paths[co] = files[co].getAbsolutePath();

        MediaScannerConnection.scanFile(baseContext, paths, null, null);

        // and now recursively scan subfolders
        files = new File(folderPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (int co=0; co<files.length; co++)
            scan(baseContext, files[co].getAbsolutePath());
    }


}
