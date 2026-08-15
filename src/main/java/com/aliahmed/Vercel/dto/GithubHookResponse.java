package com.aliahmed.Vercel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** GitHub's response when a webhook is created — we only keep its id. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubHookResponse(Long id) {
}
