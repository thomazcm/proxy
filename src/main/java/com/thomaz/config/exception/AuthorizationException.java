package com.thomaz.config.exception;

public class AuthorizationException extends RuntimeException {

    public AuthorizationException() {
        super();
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        return "AuthorizationException{" +
                "message=" + getMessage() +
                ", cause=" + getCause() +
                '}';
    }
}
