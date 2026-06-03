package com.easyconnect.nas.data.repository;

import android.os.Environment;

import com.easyconnect.nas.data.model.NasFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LocalFileRepository {

    public List<NasFile> listFiles(String path) {
        List<NasFile> files = new ArrayList<>();
        File dir = new File(path);

        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }

        File[] fileArray = dir.listFiles();
        if (fileArray == null) {
            return files;
        }

        Arrays.sort(fileArray, (a, b) -> {
            // Directories first, then by name
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File file : fileArray) {
            if (file.isHidden()) continue;

            NasFile nasFile = new NasFile();
            nasFile.setName(file.getName());
            nasFile.setFullPath(file.getAbsolutePath());
            nasFile.setDirectory(file.isDirectory());
            nasFile.setSize(file.isFile() ? file.length() : 0);
            nasFile.setLastModified(new Date(file.lastModified()));
            files.add(nasFile);
        }

        return files;
    }

    public String getExternalStoragePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public String getDownloadsPath() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }
}
