package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.datastore.InMemoryDataStore;
import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Part 2 – Room Management Handles GET, POST, DELETE for /api/v1/rooms and
 * /api/v1/rooms/{roomId}
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    // ----------------------------------------------------------------
    // GET /api/v1/rooms — list all rooms
    // ----------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(InMemoryDataStore.rooms.values());
        return Response.ok(roomList).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/rooms — create a new room
    // ----------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room 'id' field is required."))
                    .build();
        }
        if (InMemoryDataStore.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Room '" + room.getId() + "' already exists."))
                    .build();
        }

        InMemoryDataStore.rooms.put(room.getId(), room);

        URI location = UriBuilder.fromPath("/api/v1/rooms/{id}").build(room.getId());
        return Response.created(location)
                .entity(Map.of(
                        "message", "Room created successfully.",
                        "room", room,
                        "_links", Map.of("self", "/api/v1/rooms/" + room.getId())
                ))
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/rooms/{roomId} — get a single room
    // ----------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = InMemoryDataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId} — decommission a room
    // ----------------------------------------------------------------
    /**
     * Blocks deletion if sensors are still assigned (throws 409).
     *
     * IDEMPOTENCY: DELETE is idempotent — the server state after N identical
     * DELETE calls is the same as after the first. First call removes the room
     * (200 OK). Subsequent calls find nothing (404 Not Found). The underlying
     * state — room absent — is identical both times.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = InMemoryDataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found: " + roomId))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        InMemoryDataStore.rooms.remove(roomId);
        return Response.ok(Map.of(
                "message", "Room '" + roomId + "' deleted successfully."
        )).build();
    }
}
