package com.westminster.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1.2 – Discovery endpoint at GET /api/v1
 * Returns API metadata and HATEOAS navigation links.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("name",       "Smart Campus Admin");
        contact.put("email",      "smartcampus@westminster.ac.uk");
        contact.put("university", "University of Westminster");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api",       "Smart Campus Sensor & Room Management API");
        body.put("version",   "1.0.0");
        body.put("status",    "running");
        body.put("contact",   contact);
        body.put("_links",    links);
        body.put("resources", resources);

        return Response.ok(body).build();
    }
}