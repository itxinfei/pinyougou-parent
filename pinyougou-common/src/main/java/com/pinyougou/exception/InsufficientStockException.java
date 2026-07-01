package com.pinyougou.exception;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(String message) {
        super("409", message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super("409", message, cause);
    }
}
