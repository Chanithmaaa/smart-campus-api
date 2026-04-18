package com.westminster.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 5.3 – Maps SensorUnavailableException → HTTP 403 Forbidden Triggered
 * when POST /sensors/{id}/readings is called on a MAINTENANCE sensor.
 */
@Provider
public class SensorUnavailableMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("sensorId", ex.getSensorId());
        body.put("sensorStatus", ex.getCurrentStatus());
        body.put("resolution",
                "Set sensor status to 'ACTIVE' before submitting new readings.");
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}
