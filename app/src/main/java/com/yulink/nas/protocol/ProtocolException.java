package com.yulink.nas.protocol;

public class ProtocolException extends Exception {
    private int errorCode;

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtocolException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProtocolException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
