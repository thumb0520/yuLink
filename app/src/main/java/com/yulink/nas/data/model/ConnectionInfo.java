package com.yulink.nas.data.model;

public class ConnectionInfo {
    private long id;
    private String name;
    private ProtocolType protocol;
    private String host;
    private int port;
    private String username;
    private String password;
    private String shareName;
    private String defaultPath;
    private boolean useSmbEncryption;
    private boolean useFtps;
    private boolean passiveMode;
    private String sshKeyPath;

    public ConnectionInfo() {}

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProtocolType getProtocol() { return protocol; }
    public void setProtocol(ProtocolType protocol) { this.protocol = protocol; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getShareName() { return shareName; }
    public void setShareName(String shareName) { this.shareName = shareName; }

    public String getDefaultPath() { return defaultPath; }
    public void setDefaultPath(String defaultPath) { this.defaultPath = defaultPath; }

    public boolean isUseSmbEncryption() { return useSmbEncryption; }
    public void setUseSmbEncryption(boolean useSmbEncryption) { this.useSmbEncryption = useSmbEncryption; }

    public boolean isUseFtps() { return useFtps; }
    public void setUseFtps(boolean useFtps) { this.useFtps = useFtps; }

    public boolean isPassiveMode() { return passiveMode; }
    public void setPassiveMode(boolean passiveMode) { this.passiveMode = passiveMode; }

    public String getSshKeyPath() { return sshKeyPath; }
    public void setSshKeyPath(String sshKeyPath) { this.sshKeyPath = sshKeyPath; }
}
