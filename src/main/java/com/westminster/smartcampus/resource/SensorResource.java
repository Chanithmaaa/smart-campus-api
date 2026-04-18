package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.datastore.InMemoryDataStore;
import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 & 4 – Sensor Operations Handles POST, GET (with ?type= filter) for
 * /api/v1/sensors Provides sub-resource locator for
 * /api/v1/sensors/{sensorId}/readings
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // ----------------------------------------------------------------
    // GET /api/v1/sensors?type=CO2 — list sensors, optional type filter
    // ----------------------------------------------------------------
    /**
     * @QueryParam is preferred over path-based filtering (/sensors/type/CO2)
     * because query params semantically represent optional search criteria,
     * while path segments identify specific resources. Multiple filters also
     * compose naturally: ?type=CO2&status=ACTIVE
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(InMemoryDataStore.sensors.values());

        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors — register a sensor (validates roomId exists)
    // ----------------------------------------------------------------
    /**
     * @Consumes(APPLICATION_JSON): JAX-RS rejects requests with a different
     * Content-Type (e.g. text/plain) with HTTP 415 Unsupported Media Type — the
     * method body is never reached.
     */
    @POST
    public Response registerSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor 'id' field is required."))
                    .build();
        }
        if (InMemoryDataStore.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Sensor '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Validate roomId exists before accepting the sensor
        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "'roomId' is required."))
                    .build();
        }
        Room room = InMemoryDataStore.rooms.get(roomId);
        if (room == null) {
            // 422: JSON is valid but the roomId reference inside it doesn't exist
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        InMemoryDataStore.sensors.put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());

        URI location = UriBuilder.fromPath("/api/v1/sensors/{id}").build(sensor.getId());
        return Response.created(location)
                .entity(Map.of(
                        "message", "Sensor registered successfully.",
                        "sensor", sensor,
                        "_links", Map.of(
                                "self", "/api/v1/sensors/" + sensor.getId(),
                                "readings", "/api/v1/sensors/" + sensor.getId() + "/readings",
                                "room", "/api/v1/rooms/" + sensor.getRoomId()
                        )
                ))
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId} — single sensor
    // ----------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = InMemoryDataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ----------------------------------------------------------------
    // Part 4.1 – Sub-Resource Locator for /sensors/{sensorId}/readings
    // ----------------------------------------------------------------
    /**
     * This method has no HTTP verb annotation — it is a sub-resource locator.
     * JAX-RS calls it to get the delegate object, then dispatches the actual
     * HTTP method (GET/POST) to SensorReadingResource.
     *
     * This keeps SensorResource focused on sensor-level concerns and delegates
     * all reading logic to a dedicated, independently testable class.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = InMemoryDataStore.sensors.get(sensorId);
        if (sensor == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Sensor not found: " + sensorId))
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }
        return new SensorReadingResource(sensorId);
    }
}
