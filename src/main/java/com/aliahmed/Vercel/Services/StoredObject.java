package com.aliahmed.Vercel.Services;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * A file resolved for serving, independent of where it's stored. Local disk and
 * R2 both produce one of these, so the site controller doesn't care which
 * backend is active.
 */
public record StoredObject(Resource resource, MediaType contentType) {
}
