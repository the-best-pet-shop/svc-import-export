package com.thebestpetshop.importexport.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    @GET public Response health() { return Response.ok(java.util.Map.of("status", "UP", "service", "svc-import-export")).build(); }
}
