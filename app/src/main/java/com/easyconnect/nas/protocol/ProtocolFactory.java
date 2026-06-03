package com.easyconnect.nas.protocol;

import com.easyconnect.nas.data.model.ProtocolType;
import com.easyconnect.nas.protocol.ftp.FtpProtocolManager;
import com.easyconnect.nas.protocol.sftp.SftpProtocolManager;
import com.easyconnect.nas.protocol.smb.SmbProtocolManager;

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
