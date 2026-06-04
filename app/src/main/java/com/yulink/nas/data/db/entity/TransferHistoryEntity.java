package com.yulink.nas.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transfer_history")
public class TransferHistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long connectionId;
    public String fileName;
    public String sourcePath;
    public String destinationPath;
    public long fileSize;
    public long transferredBytes;
    public int direction; // 0=upload, 1=download
    public int status;    // 0=pending, 1=running, 2=completed, 3=failed
    public String errorMessage;
    public long startedAt;
    public long completedAt;
}
