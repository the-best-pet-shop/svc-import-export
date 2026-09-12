package com.thebestpetshop.importexport.controller;

import com.thebestpetshop.importexport.config.AccessContext;
import com.thebestpetshop.importexport.exception.ImportExportException;
import com.thebestpetshop.importexport.model.dto.ApproveBatchRequest;
import com.thebestpetshop.importexport.model.dto.CreateBatchRequest;
import com.thebestpetshop.importexport.model.dto.ExportRequest;
import com.thebestpetshop.importexport.service.ImportExportService;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;

@Path("/internal/import-export")
@Produces(MediaType.APPLICATION_JSON)
public class ImportExportResource {
    private final ImportExportService service;
    public ImportExportResource(ImportExportService service) { this.service = service; }

    @POST @Path("/batches")
    public Response create(@Valid CreateBatchRequest request, @Context HttpHeaders headers, @Context UriInfo uri) {
        var context = context(headers); var result = service.create(request, headers.getHeaderString("Idempotency-Key"), context); return Response.accepted(result).location(uri.getAbsolutePathBuilder().path(result.id().toString()).build()).build();
    }
    @GET @Path("/batches")
    public Response list(@Context HttpHeaders headers) { return Response.ok(service.list(context(headers))).build(); }
    @GET @Path("/batches/{id}")
    public Response get(@PathParam("id") UUID id, @Context HttpHeaders headers) { return Response.ok(service.get(id, context(headers))).build(); }
    @POST @Path("/batches/{id}/run")
    public Response run(@PathParam("id") UUID id, @Context HttpHeaders headers) { return Response.ok(service.process(id, context(headers))).build(); }
    @POST @Path("/batches/{id}/approve")
    public Response approve(@PathParam("id") UUID id, @Valid ApproveBatchRequest request, @Context HttpHeaders headers) { return Response.ok(service.approve(id, request, context(headers))).build(); }
    @POST @Path("/batches/{id}/rollback")
    public Response rollback(@PathParam("id") UUID id, @Context HttpHeaders headers) { return Response.ok(service.rollback(id, context(headers))).build(); }
    @POST @Path("/exports")
    public Response export(@Valid ExportRequest request, @Context HttpHeaders headers) { return Response.accepted(service.export(request, context(headers))).build(); }
    @GET @Path("/exports/{id}/download")
    @Produces("text/csv")
    public Response download(@PathParam("id") UUID id, @Context HttpHeaders headers) { return Response.ok(service.download(id, headers.getHeaderString("X-Download-Token"), context(headers))).header("Content-Disposition", "attachment; filename=export.csv").build(); }

    private static AccessContext context(HttpHeaders headers) {
        if (headers.getHeaderString(HttpHeaders.AUTHORIZATION) == null || headers.getHeaderString(HttpHeaders.AUTHORIZATION).isBlank()) throw ImportExportException.unauthorized("bearer token is required");
        try {
            var organization = UUID.fromString(required(headers, "X-Organization-Id")); var unit = optional(headers, "X-Unit-Id"); var actor = optional(headers, "X-Actor-Id");
            var scope = headers.getHeaderString("X-Scope"); if (scope == null || scope.isBlank()) scope = "import:read import:write import:approve export:read";
            var correlation = optional(headers, "X-Correlation-Id"); return new AccessContext(actor == null ? UUID.nameUUIDFromBytes(organization.toString().getBytes()) : actor, organization, unit, scope, correlation == null ? UUID.randomUUID() : correlation);
        } catch (IllegalArgumentException e) { throw ImportExportException.badRequest("organization, unit, actor and correlation headers must be UUIDs"); }
    }
    private static String required(HttpHeaders headers, String name) { var value = headers.getHeaderString(name); if (value == null || value.isBlank()) throw ImportExportException.unauthorized(name + " header is required"); return value; }
    private static UUID optional(HttpHeaders headers, String name) { var value = headers.getHeaderString(name); return value == null || value.isBlank() ? null : UUID.fromString(value); }
}
