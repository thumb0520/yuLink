package com.yulink.nas.data.model;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class NasFile {
    private String name;
    private String fullPath;
    private boolean isDirectory;
    private long size;
    private Date lastModified;
    private String permissions;
    private String mimeType;

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "tiff")
    );
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(
            Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp")
    );
    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(
            Arrays.asList("mp3", "flac", "wav", "aac", "ogg", "wma", "m4a", "opus")
    );

    public NasFile() {}

    public NasFile(String name, String fullPath, boolean isDirectory, long size, Date lastModified) {
        this.name = name;
        this.fullPath = fullPath;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getFileExtension() {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    public boolean isImage() {
        return IMAGE_EXTENSIONS.contains(getFileExtension());
    }

    public boolean isVideo() {
        return VIDEO_EXTENSIONS.contains(getFileExtension());
    }

    public boolean isAudio() {
        return AUDIO_EXTENSIONS.contains(getFileExtension());
    }

    public boolean isPreviewable() {
        return isImage() || isVideo() || isAudio();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
