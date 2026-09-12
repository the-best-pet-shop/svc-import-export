package com.thebestpetshop.importexport.model;

import java.time.Instant;
import java.util.UUID;

public record ExportResult(UUID exportId, String purpose, String format, String downloadUrl, String checksum, Instant expiresAt, boolean piiMasked) {}
