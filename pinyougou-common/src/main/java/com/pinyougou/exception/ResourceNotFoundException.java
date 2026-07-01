package com.pinyougou.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super("404", message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super("404", message, cause);
    }
}
