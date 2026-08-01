package com.aliahmed.Vercel.dto;

public record ExchangeCodeRequest(String code) {

    public ExchangeCodeRequest {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        code = code.trim();
    }
}
