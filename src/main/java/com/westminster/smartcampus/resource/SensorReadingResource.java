package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.datastore.InMemoryDataStore;
import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Part 4.2 – Historical Data Management Handles GET / and POST / for
 * /api/v1/sensors/{sensorId}/readings
 *
 * Instantiated by SensorResource via the sub-resource locator pattern. Receives
 * sensorId as constructor context — clean, testable, focused.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings — full history
    // ----------------------------------------------------------------
    @GET
    public Response getReadings() {
        List<SensorReading> history = InMemoryDataStore.readings
                .getOrDefault(sensorId, new CopyOnWriteArrayList<>());
        return Response.ok(history).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings — append new reading
    // ----------------------------------------------------------------
    /**
     * Side effect: updates the parent Sensor's currentValue to ensure
     * consistency across the API — sensor always reflects latest measurement.
     *
     * Throws SensorUnavailableException (→ 403) if status is MAINTENANCE.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = InMemoryDataStore.sensors.get(sensorId);

        // Business rule: MAINTENANCE sensors cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist reading
        InMemoryDataStore.readings
                .computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>())
                .add(reading);

        // Side effect: keep parent sensor current
        sensor.setCurrentValue(reading.getValue());

        URI location = UriBuilder.fromPath("/api/v1/sensors/{s}/readings/{r}")
                .build(sensorId, reading.getId());

        return Response.created(location)
                .entity(Map.of(
                        "message", "Reading recorded successfully.",
                        "reading", reading,
                        "updatedSensorValue", sensor.getCurrentValue(),
                        "_links", Map.of(
                                "sensor", "/api/v1/sensors/" + sensorId,
                                "readings", "/api/v1/sensors/" + sensorId + "/readings"
                        )
                ))
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    // ----------------------------------------------------------------
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        List<SensorReading> history = InMemoryDataStore.readings
                .getOrDefault(sensorId, new CopyOnWriteArrayList<>());

        return history.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Reading not found: " + readingId))
                        .build());
    }
}
