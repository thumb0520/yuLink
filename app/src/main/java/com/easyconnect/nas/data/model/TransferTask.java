package com.easyconnect.nas.data.model;

import java.util.UUID;

public class TransferTask {
    public enum Direction { UPLOAD, DOWNLOAD }
    public enum Status { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

    private String taskId;
    private long connectionId;
    private Direction direction;
    private String sourcePath;
    private String destinationPath;
    private String fileName;
    private long totalBytes;
    private long transferredBytes;
    private Status status;
    private String errorMessage;
    private int notificationId;

    public TransferTask() {
        this.taskId = UUID.randomUUID().toString();
        this.status = Status.QUEUED;
    }

    public TransferTask(long connectionId, Direction direction, String sourcePath,
                        String destinationPath, String fileName, long totalBytes) {
        this();
        this.connectionId = connectionId;
        this.direction = direction;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.fileName = fileName;
        this.totalBytes = totalBytes;
    }

    public int getProgressPercent() {
        if (totalBytes <= 0) return 0;
        return (int) ((transferredBytes * 100) / totalBytes);
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public long getConnectionId() { return connectionId; }
    public void setConnectionId(long connectionId) { this.connectionId = connectionId; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getDestinationPath() { return destinationPath; }
    public void setDestinationPath(String destinationPath) { this.destinationPath = destinationPath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

    public long getTransferredBytes() { return transferredBytes; }
    public void setTransferredBytes(long transferredBytes) { this.transferredBytes = transferredBytes; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
}
