package com.thebestpetshop.importexport.model;

import java.time.Instant;
import java.util.Map;

public record ProblemDetails(int status, String code, String message, Map<String, Object> details, String correlationId, Instant timestamp) {}
