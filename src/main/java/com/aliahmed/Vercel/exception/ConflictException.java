package com.aliahmed.Vercel.exception;

/** A request that clashes with existing state, e.g. connecting a repo twice. Maps to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
