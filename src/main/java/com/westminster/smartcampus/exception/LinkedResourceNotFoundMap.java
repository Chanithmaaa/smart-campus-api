package com.westminster.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 5.2 – Maps LinkedResourceNotFoundException → HTTP 422 Unprocessable
 * Entity
 *
 * 422 is more accurate than 404 here because: - The request URL
 * (/api/v1/sensors) is valid and found. - The JSON body is syntactically
 * correct. - The problem is a broken reference INSIDE the payload (roomId
 * doesn't exist). - 422 signals a semantic/data-integrity failure, not a
 * missing endpoint.
 */
@Provider
public class LinkedResourceNotFoundMap implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("message", ex.getMessage());
        body.put("missingResourceType", ex.getResourceType());
        body.put("missingResourceId", ex.getResourceId());
        body.put("hint", "Create the " + ex.getResourceType()
                + " with id '" + ex.getResourceId() + "' first, then retry.");
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}
