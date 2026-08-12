package com.aliahmed.Vercel.dto;

import com.aliahmed.Vercel.util.ProjectPaths;

/**
 * Update a connected project's build settings. Currently just the root directory
 * (the subfolder to build in), so an existing project can be pointed at "Client"
 * without reconnecting. Null/blank clears it back to auto-detect.
 */
public record UpdateProjectRequest(String rootDirectory) {

    public UpdateProjectRequest {
        rootDirectory = ProjectPaths.normalizeRootDirectory(rootDirectory);
    }
}
