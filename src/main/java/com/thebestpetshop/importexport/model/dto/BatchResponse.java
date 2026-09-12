package com.thebestpetshop.importexport.model.dto;

import com.thebestpetshop.importexport.model.BatchPreview;
import com.thebestpetshop.importexport.model.BatchStatus;

import java.time.Instant;
import java.util.UUID;

public record BatchResponse(UUID id, UUID organizationId, UUID unitId, String manifestId, int manifestVersion,
                            String purpose, BatchStatus status, boolean dryRun, String checksum, long rows,
                            long validRows, long invalidRows, long conflicts, String checkpoint, Instant createdAt,
                            Instant updatedAt, BatchPreview preview) {}
