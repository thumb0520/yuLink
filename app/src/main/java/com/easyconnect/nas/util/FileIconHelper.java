package com.easyconnect.nas.util;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.model.NasFile;

public class FileIconHelper {

    public static int getFileIcon(NasFile file) {
        if (file.isDirectory()) {
            return R.drawable.ic_folder;
        }

        String ext = file.getFileExtension();
        if (ext == null || ext.isEmpty()) {
            return android.R.drawable.ic_menu_info_details;
        }

        switch (ext) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "webp":
            case "bmp":
            case "svg":
                return android.R.drawable.ic_menu_gallery;

            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
            case "wmv":
            case "flv":
            case "webm":
                return android.R.drawable.ic_media_play;

            case "mp3":
            case "flac":
            case "wav":
            case "aac":
            case "ogg":
            case "wma":
            case "m4a":
                return android.R.drawable.ic_lock_silent_mode_off;

            case "pdf":
                return android.R.drawable.ic_menu_agenda;

            case "doc":
            case "docx":
            case "txt":
            case "rtf":
            case "odt":
                return android.R.drawable.ic_menu_edit;

            case "xls":
            case "xlsx":
            case "csv":
                return android.R.drawable.ic_menu_sort_by_size;

            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return android.R.drawable.ic_menu_save;

            case "apk":
                return android.R.drawable.ic_menu_add;

            default:
                return android.R.drawable.ic_menu_info_details;
        }
    }

    public static int getFileColor(NasFile file) {
        if (file.isDirectory()) {
            return R.color.file_folder;
        }

        if (file.isImage()) return R.color.file_image;
        if (file.isVideo()) return R.color.file_video;
        if (file.isAudio()) return R.color.file_audio;

        String ext = file.getFileExtension();
        if (ext == null) return R.color.file_default;

        switch (ext) {
            case "pdf":
            case "doc":
            case "docx":
            case "txt":
            case "xls":
            case "xlsx":
                return R.color.file_document;
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return R.color.file_archive;
            default:
                return R.color.file_default;
        }
    }
}
