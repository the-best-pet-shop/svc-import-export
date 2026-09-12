package com.thebestpetshop.importexport.exception;

public class ImportExportException extends RuntimeException {
    private final int status;
    private final String code;
    private ImportExportException(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    public int status() { return status; }
    public String code() { return code; }
    public static ImportExportException badRequest(String m) { return new ImportExportException(400, "BAD_REQUEST", m); }
    public static ImportExportException unauthorized(String m) { return new ImportExportException(401, "UNAUTHORIZED", m); }
    public static ImportExportException forbidden(String m) { return new ImportExportException(403, "FORBIDDEN", m); }
    public static ImportExportException notFound(String m) { return new ImportExportException(404, "NOT_FOUND", m); }
    public static ImportExportException conflict(String m) { return new ImportExportException(409, "CONFLICT", m); }
    public static ImportExportException tooLarge(String m) { return new ImportExportException(413, "PAYLOAD_TOO_LARGE", m); }
    public static ImportExportException invalid(String m) { return new ImportExportException(422, "INVALID_IMPORT", m); }
    public static ImportExportException rateLimited(String m) { return new ImportExportException(429, "RATE_LIMITED", m); }
    public static ImportExportException unavailable(String m) { return new ImportExportException(503, "DEPENDENCY_UNAVAILABLE", m); }
}
