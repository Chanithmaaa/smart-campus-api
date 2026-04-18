package com.westminster.smartcampus.config;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application subclass.
 *
 * @ApplicationPath sets the versioned base URI for all resources. Jersey
 * auto-discovers all @Path, @Provider classes on the classpath.
 *
 * Lifecycle note: By default JAX-RS creates a NEW resource instance per request
 * (request-scoped). Shared state must therefore live in static, thread-safe
 * structures — see InMemoryDataStore.
 */
@ApplicationPath("/api/v1")
public class ApiApplication extends Application {
    // Empty — Jersey scans all packages automatically via ResourceConfig in Main
}
