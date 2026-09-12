package com.thebestpetshop.importexport.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveBatchRequest(@NotBlank String approvalReason, @NotBlank String checksum) {}
