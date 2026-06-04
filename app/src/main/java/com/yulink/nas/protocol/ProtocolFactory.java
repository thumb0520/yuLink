package com.yulink.nas.protocol;

import com.yulink.nas.data.model.ProtocolType;
import com.yulink.nas.protocol.ftp.FtpProtocolManager;
import com.yulink.nas.protocol.sftp.SftpProtocolManager;
import com.yulink.nas.protocol.smb.SmbProtocolManager;

public class ProtocolFactory {

    public static ProtocolManager create(ProtocolType type) {
        switch (type) {
            case SMB:
                return new SmbProtocolManager();
            case FTP:
                return new FtpProtocolManager();
            case SFTP:
                return new SftpProtocolManager();
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + type);
        }
    }
}
