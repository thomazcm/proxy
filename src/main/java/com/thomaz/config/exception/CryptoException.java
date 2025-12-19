package com.thomaz.config.exception;

public class CryptoException extends RuntimeException {
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "CryptoException{" +
                "message=" + getMessage() +
                ", cause=" + getCause() +
                '}';
    }
}
