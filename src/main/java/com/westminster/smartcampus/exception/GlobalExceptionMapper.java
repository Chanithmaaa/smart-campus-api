package com.westminster.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 – Global Safety Net: ExceptionMapper<Throwable>
 *
 * Catches ALL unhandled exceptions. Ensures no raw stack trace or default
 * server error page is ever returned to a client.
 *
 * Security rationale: stack traces expose internal package paths, library
 * versions and logic flow — information attackers use to find known CVEs, craft
 * injections, and map application internals. Full details are logged
 * server-side only.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER
            = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable throwable) {
        LOGGER.log(Level.SEVERE,
                "Unhandled exception caught by GlobalExceptionMapper", throwable);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message",
                "An unexpected error occurred. Please contact the API administrator.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}
