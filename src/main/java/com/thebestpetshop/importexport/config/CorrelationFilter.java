package com.thebestpetshop.importexport.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

@Provider
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String PROPERTY = "import-export.correlation";
    @Override public void filter(ContainerRequestContext context) {
        var value = context.getHeaderString("X-Correlation-Id");
        context.setProperty(PROPERTY, value == null || value.isBlank() ? UUID.randomUUID().toString() : value);
    }
    @Override public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("X-Correlation-Id", request.getProperty(PROPERTY));
    }
}
