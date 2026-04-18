package com.westminster.smartcampus.config;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Bootstraps an embedded Grizzly HTTP server. No external servlet container
 * (Tomcat/Jetty) required.
 *
 * Build : mvn clean package Run : java -jar target/smart-campus-api-1.0.0.jar
 * API : http://localhost:8080/api/v1
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig()
                .packages("com.westminster.smartcampus")
                // THIS IS THE NEW LINE: It turns on Jackson JSON translation!
                .register(org.glassfish.jersey.jackson.JacksonFeature.class); 
                
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        LOGGER.info("Smart Campus API running at http://localhost:8080/api/v1");
        LOGGER.info("Press ENTER to stop.");
        System.in.read();
        server.shutdownNow();
    }
}