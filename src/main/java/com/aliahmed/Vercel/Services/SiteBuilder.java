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

    /**
     * Produces (or locates) the folder of static files to serve.
     *
     * @param source   the folder to build
     * @param basePath the URL path the site is served under (e.g. {@code "/sites/app-x1y2/"}),
     *                 so a bundler bakes in correct absolute asset paths and the app can read
     *                 it back for its router base. Ignored by builders that don't recompile.
     * @param buildLog collects the build's output (commands, npm/bundler logs) so it can be
     *                 stored and shown; the full output is appended even when the build fails.
     */
    Path build(Path source, String basePath, StringBuilder buildLog);
}
