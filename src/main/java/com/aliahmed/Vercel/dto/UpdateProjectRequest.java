package com.aliahmed.Vercel.dto;

/**
 * Update a connected project's build settings. Fields left out (null) are kept as-is;
 * a field sent as blank ("") clears it. So you can change {@code rootDirectory} or
 * {@code defaultPath} independently without resending the other. Values are validated
 * and normalised in the service, not here, so "not sent" stays distinguishable from "clear".
 */
public record UpdateProjectRequest(String rootDirectory, String defaultPath) {
}
