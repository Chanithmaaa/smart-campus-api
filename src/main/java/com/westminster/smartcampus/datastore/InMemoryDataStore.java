package com.westminster.smartcampus.datastore;

import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static in-memory data store.
 *
 * Because JAX-RS is request-scoped (new resource instance per request), all
 * shared mutable state MUST be stored here as static fields.
 *
 * ConcurrentHashMap prevents race conditions when multiple threads read/write
 * simultaneously — unlike plain HashMap which is not thread-safe.
 * CopyOnWriteArrayList gives safe iteration over reading history lists.
 */
public class InMemoryDataStore {

    // Rooms: roomId -> Room
    public static final ConcurrentHashMap<String, Room> rooms
            = new ConcurrentHashMap<>();

    // Sensors: sensorId -> Sensor
    public static final ConcurrentHashMap<String, Sensor> sensors
            = new ConcurrentHashMap<>();

    // Readings: sensorId -> list of SensorReading
    public static final ConcurrentHashMap<String, CopyOnWriteArrayList<SensorReading>> readings
            = new ConcurrentHashMap<>();

    private InMemoryDataStore() {
    }
}
