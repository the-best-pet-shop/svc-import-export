package com.thebestpetshop.importexport.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record ImportManifest(
        @NotBlank String manifestId,
        @Min(1) int version,
        @NotBlank String purpose,
        @NotNull FileFormat format,
        @NotBlank String encoding,
        @NotBlank String timezone,
        @NotNull ScopePolicy scopePolicy,
        @NotNull UUID organizationId,
        UUID unitId,
        @NotEmpty List<@Valid ManifestColumn> columns) {
    public ImportManifest {
        if (version < 1) throw new IllegalArgumentException("manifest version must be positive");
        try { ZoneId.of(timezone); } catch (RuntimeException e) { throw new IllegalArgumentException("timezone must be an IANA timezone"); }
        if (scopePolicy == ScopePolicy.UNIT_LINKED && unitId == null) throw new IllegalArgumentException("unitId is required for UNIT_LINKED manifests");
    }
}
