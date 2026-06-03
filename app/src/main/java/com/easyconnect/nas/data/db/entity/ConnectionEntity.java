package com.easyconnect.nas.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.easyconnect.nas.data.db.converter.ProtocolTypeConverter;
import com.easyconnect.nas.data.model.ProtocolType;

@Entity(tableName = "connections")
@TypeConverters(ProtocolTypeConverter.class)
public class ConnectionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public ProtocolType protocol = ProtocolType.SMB;

    @NonNull
    public String host = "";

    public int port;

    @NonNull
    public String username = "";

    @NonNull
    public String encryptedPassword = "";

    public String shareName;
    public String defaultPath;
    public boolean useSmbEncryption;
    public boolean useFtps;
    public boolean passiveMode;
    public String sshKeyPath;
    public long createdAt;
    public long lastConnectedAt;
}
