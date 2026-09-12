package com.thebestpetshop.importexport.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExportRequest(@NotBlank String purpose, @NotBlank String format, @NotNull UUID organizationId,
                            UUID unitId, List<Map<String, String>> rows, boolean includePii,
                            @NotBlank String timezone) {}
