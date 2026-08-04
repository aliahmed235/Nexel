package com.aliahmed.Vercel.Services;

import java.nio.file.Path;

/**
 * Turns an unpacked repository into a folder of static files ready to serve.
 *
 * <p>The one step that differs between site types. {@link StaticSiteBuilder}
 * treats the repo as already-built. A future {@code NodeDockerBuilder} would
 * run {@code npm install && npm run build} in a container and return the
 * generated output — everything around this interface stays the same.
 */
public interface SiteBuilder {

    /** Whether this builder can handle the given repository. */
    boolean supports(Path source);

    /** Produces (or locates) the folder of static files to serve. */
    Path build(Path source);
}
