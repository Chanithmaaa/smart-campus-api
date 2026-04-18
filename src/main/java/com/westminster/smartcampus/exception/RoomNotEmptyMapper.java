package com.westminster.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 5.1 – Maps RoomNotEmptyException → HTTP 409 Conflict Triggered when
 * DELETE /rooms/{id} is called on a room with sensors.
 */
@Provider
public class RoomNotEmptyMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("roomId", ex.getRoomId());
        body.put("activeSensors", ex.getSensorCount());
        body.put("resolution",
                "Remove or reassign all sensors from room '"
                + ex.getRoomId() + "' before deleting it.");
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}
