package com.aliahmed.Vercel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubEmailResponse(
        String email,
        boolean primary,
        boolean verified
) {
}
