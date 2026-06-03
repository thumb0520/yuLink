package com.easyconnect.nas.util;

import java.util.HashMap;
import java.util.Map;

public class MimeTypeHelper {
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        // Images
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("webp", "image/webp");
        MIME_TYPES.put("bmp", "image/bmp");
        MIME_TYPES.put("svg", "image/svg+xml");

        // Videos
        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("mkv", "video/x-matroska");
        MIME_TYPES.put("avi", "video/x-msvideo");
        MIME_TYPES.put("mov", "video/quicktime");
        MIME_TYPES.put("wmv", "video/x-ms-wmv");
        MIME_TYPES.put("flv", "video/x-flv");
        MIME_TYPES.put("webm", "video/webm");
        MIME_TYPES.put("3gp", "video/3gpp");

        // Audio
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("flac", "audio/flac");
        MIME_TYPES.put("wav", "audio/wav");
        MIME_TYPES.put("aac", "audio/aac");
        MIME_TYPES.put("ogg", "audio/ogg");
        MIME_TYPES.put("wma", "audio/x-ms-wma");
        MIME_TYPES.put("m4a", "audio/mp4");
        MIME_TYPES.put("opus", "audio/opus");

        // Documents
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("doc", "application/msword");
        MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPES.put("xls", "application/vnd.ms-excel");
        MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("htm", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("xml", "application/xml");
        MIME_TYPES.put("csv", "text/csv");

        // Archives
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("rar", "application/x-rar-compressed");
        MIME_TYPES.put("7z", "application/x-7z-compressed");
        MIME_TYPES.put("tar", "application/x-tar");
        MIME_TYPES.put("gz", "application/gzip");

        // APK
        MIME_TYPES.put("apk", "application/vnd.android.package-archive");
    }

    public static String getMimeType(String extension) {
        if (extension == null) return "application/octet-stream";
        String ext = extension.toLowerCase();
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    public static String getMimeTypeFromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        return getMimeType(ext);
    }
}
