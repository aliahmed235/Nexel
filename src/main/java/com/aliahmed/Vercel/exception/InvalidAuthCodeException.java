package com.aliahmed.Vercel.exception;

public class InvalidAuthCodeException extends RuntimeException {

    public InvalidAuthCodeException(String message) {
        super(message);
    }
}
