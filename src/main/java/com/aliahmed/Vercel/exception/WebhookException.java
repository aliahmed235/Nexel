package com.aliahmed.Vercel.exception;

/**
 * A GitHub webhook delivery that can't be trusted — a missing, malformed, or
 * mismatched signature. Surfaces as 401 so a spoofed push is rejected outright.
 */
public class WebhookException extends RuntimeException {

    public WebhookException(String message) {
        super(message);
    }

    public WebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
