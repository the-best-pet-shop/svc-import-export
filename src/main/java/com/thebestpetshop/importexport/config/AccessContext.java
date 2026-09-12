package com.thebestpetshop.importexport.config;

import com.thebestpetshop.importexport.exception.ImportExportException;

import java.util.UUID;

public record AccessContext(UUID actorId, UUID organizationId, UUID unitId, String scope, UUID correlationId) {
    public void requireOrganization(UUID requested) {
        if (requested == null || organizationId == null || !organizationId.equals(requested)) throw ImportExportException.forbidden("organization scope does not match access context");
    }
    public void requireUnit(UUID requested) {
        if (requested == null || unitId == null || !unitId.equals(requested)) throw ImportExportException.forbidden("unit scope is required and does not match access context");
    }
    public void requireScope(String expected) {
        if (scope == null || java.util.Arrays.stream(scope.split("[,\\s]+"))
                .noneMatch(expected::equals)) throw ImportExportException.forbidden("required scope is missing");
    }
}
