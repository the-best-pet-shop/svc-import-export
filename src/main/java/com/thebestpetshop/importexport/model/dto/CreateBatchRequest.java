package com.thebestpetshop.importexport.model.dto;

import com.thebestpetshop.importexport.model.ImportManifest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBatchRequest(@NotNull @Valid ImportManifest manifest, @NotBlank String fileName,
                                 @NotBlank String contentType, @NotBlank String encoding,
                                 @NotBlank String checksum, @NotBlank String uploadSignature,
                                 @NotBlank String content, boolean dryRun) {}
