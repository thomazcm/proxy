package com.thomaz.config;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException() {
        super();
    }

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        return "InvalidRequestException{" +
                "message=" + getMessage() +
                ", cause=" + getCause() +
                '}';
    }
}
