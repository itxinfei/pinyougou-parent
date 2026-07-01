package com.pinyougou.exception;

public class SerializationException extends BusinessException {
    public SerializationException(String message) {
        super("500", message);
    }

    public SerializationException(String message, Throwable cause) {
        super("500", message, cause);
    }
}
