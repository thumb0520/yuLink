package com.easyconnect.nas.data.model;

public enum ProtocolType {
    SMB("SMB/CIFS", 445),
    FTP("FTP", 21),
    SFTP("SFTP", 22);

    private final String displayName;
    private final int defaultPort;

    ProtocolType(String displayName, int defaultPort) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
