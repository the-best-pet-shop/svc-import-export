package com.thebestpetshop.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thebestpetshop.importexport.config.AccessContext;
import com.thebestpetshop.importexport.config.ImportExportConfig;
import com.thebestpetshop.importexport.exception.ImportExportException;
import com.thebestpetshop.importexport.model.BatchPreview;
import com.thebestpetshop.importexport.model.BatchStatus;
import com.thebestpetshop.importexport.model.ExportResult;
import com.thebestpetshop.importexport.model.ImportManifest;
import com.thebestpetshop.importexport.model.RowIssue;
import com.thebestpetshop.importexport.model.dto.ApproveBatchRequest;
import com.thebestpetshop.importexport.model.dto.BatchResponse;
import com.thebestpetshop.importexport.model.dto.CreateBatchRequest;
import com.thebestpetshop.importexport.model.dto.ExportRequest;
import com.thebestpetshop.importexport.model.entity.ExportAuditEntity;
import com.thebestpetshop.importexport.model.entity.ImportBatchEntity;
import com.thebestpetshop.importexport.parser.ParsedBatch;
import com.thebestpetshop.importexport.parser.StreamingParser;
import com.thebestpetshop.importexport.repository.ExportAuditRepository;
import com.thebestpetshop.importexport.repository.ImportBatchRepository;
import com.thebestpetshop.importexport.security.UploadSecurity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ImportExportService {
    private final ImportBatchRepository batches;
    private final ExportAuditRepository audits;
    private final StreamingParser parser;
    private final UploadSecurity uploadSecurity;
    private final ImportExportConfig config;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ImportExportService(ImportBatchRepository batches, ExportAuditRepository audits, StreamingParser parser,
                               UploadSecurity uploadSecurity, ImportExportConfig config, ObjectMapper mapper) {
        this.batches = batches; this.audits = audits; this.parser = parser; this.uploadSecurity = uploadSecurity;
        this.config = config; this.mapper = mapper; this.clock = Clock.systemUTC();
    }

    @Transactional
    public BatchResponse create(CreateBatchRequest request, String idempotencyKey, AccessContext access) {
        access.requireScope("import:write"); access.requireOrganization(request.manifest().organizationId());
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw ImportExportException.badRequest("Idempotency-Key is required");
        var existing = batches.byIdempotency(access.organizationId(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().checksum.equalsIgnoreCase(request.checksum())) throw ImportExportException.conflict("idempotency key was already used with a different checksum");
            return response(existing.get());
        }
        var manifest = request.manifest();
        if (!manifest.organizationId().equals(access.organizationId())) throw ImportExportException.forbidden("cross-organization import is rejected");
        if (manifest.scopePolicy().name().equals("UNIT_LINKED")) access.requireUnit(manifest.unitId());
        if (!request.encoding().equalsIgnoreCase(manifest.encoding())) throw ImportExportException.invalid("request encoding differs from manifest encoding");
        if (!request.contentType().equalsIgnoreCase(contentType(manifest.format().name()))) throw ImportExportException.invalid("content type differs from manifest format");
        var bytes = uploadSecurity.bytes(request.content(), manifest.format().name()); uploadSecurity.verify(access.organizationId(), request.checksum(), request.uploadSignature(), bytes); uploadSecurity.scan(bytes);
        var now = Instant.now(clock); var entity = new ImportBatchEntity(); entity.id = UUID.randomUUID(); entity.organizationId = access.organizationId(); entity.unitId = manifest.unitId();
        entity.manifestId = manifest.manifestId(); entity.manifestVersion = manifest.version(); entity.purpose = manifest.purpose(); entity.format = manifest.format().name(); entity.encoding = manifest.encoding(); entity.timezone = manifest.timezone(); entity.scopePolicy = manifest.scopePolicy().name();
        entity.checksum = request.checksum().toLowerCase(Locale.ROOT); entity.idempotencyKey = idempotencyKey; entity.sourceFile = request.fileName(); entity.contentType = request.contentType(); entity.sourceContent = request.content(); entity.status = BatchStatus.QUEUED; entity.dryRun = request.dryRun(); entity.createdBy = access.actorId(); entity.checkpointRow = 0; entity.manifestJson = json(manifest); entity.createdAt = now; entity.updatedAt = now;
        batches.persist(entity); return response(entity);
    }

    @Transactional
    public BatchResponse process(UUID id, AccessContext access) {
        access.requireScope("import:write"); var entity = scoped(id, access); if (entity.status == BatchStatus.APPROVED || entity.status == BatchStatus.ROLLED_BACK) throw ImportExportException.conflict("batch cannot be processed in its current state");
        if (entity.status == BatchStatus.SUCCEEDED && entity.checkpointRow > 0) return response(entity);
        entity.status = BatchStatus.RUNNING; entity.updatedAt = Instant.now(clock);
        try {
            var manifest = mapper.readValue(entity.manifestJson, ImportManifest.class); var result = parser.parse(entity.sourceContent, manifest, config.maxRows(), config.maxPreviewRows());
            entity.rowsSeen = result.rows(); entity.validRows = result.validRows(); entity.invalidRows = result.invalidRows(); entity.conflicts = result.conflicts(); entity.checkpointRow = result.rows(); entity.previewJson = json(result.issues()); entity.status = BatchStatus.SUCCEEDED; entity.updatedAt = Instant.now(clock); return response(entity);
        } catch (ImportExportException exception) { entity.status = BatchStatus.FAILED; entity.updatedAt = Instant.now(clock); throw exception; } catch (Exception exception) { entity.status = BatchStatus.FAILED; entity.updatedAt = Instant.now(clock); throw ImportExportException.invalid("manifest or file could not be parsed"); }
    }

    @Transactional
    public BatchResponse approve(UUID id, ApproveBatchRequest request, AccessContext access) {
        access.requireScope("import:approve"); var entity = scoped(id, access); if (entity.status != BatchStatus.SUCCEEDED) throw ImportExportException.conflict("only a succeeded preview can be approved");
        if (!entity.checksum.equalsIgnoreCase(request.checksum())) throw ImportExportException.conflict("approval checksum does not match the uploaded file");
        if (entity.invalidRows > 0 || entity.conflicts > 0) throw ImportExportException.invalid("approval is blocked while errors or conflicts remain");
        entity.approvalReason = request.approvalReason(); entity.status = BatchStatus.APPROVED; entity.approvedAt = Instant.now(clock); entity.updatedAt = entity.approvedAt; return response(entity);
    }

    @Transactional
    public BatchResponse rollback(UUID id, AccessContext access) {
        access.requireScope("import:approve"); var entity = scoped(id, access); if (entity.status != BatchStatus.APPROVED) throw ImportExportException.conflict("only an approved batch can be rolled back");
        entity.status = BatchStatus.ROLLED_BACK; entity.rolledBackAt = Instant.now(clock); entity.updatedAt = entity.rolledBackAt; return response(entity);
    }

    public BatchResponse get(UUID id, AccessContext access) { access.requireScope("import:read"); return response(scoped(id, access)); }
    public List<BatchResponse> list(AccessContext access) { access.requireScope("import:read"); return batches.find("organizationId = ?1 order by createdAt desc", access.organizationId()).list().stream().map(this::response).toList(); }

    @Transactional
    public ExportResult export(ExportRequest request, AccessContext access) {
        access.requireScope("export:read"); access.requireOrganization(request.organizationId());
        if (request.unitId() == null && requiresUnit(request.purpose())) throw ImportExportException.invalid("unitId is required for this export purpose");
        if (request.unitId() != null) access.requireUnit(request.unitId());
        try { ZoneId.of(request.timezone()); } catch (RuntimeException e) { throw ImportExportException.invalid("timezone must be an IANA timezone"); }
        var rows = request.rows() == null ? List.<Map<String, String>>of() : request.rows(); var masked = !request.includePii();
        if (request.includePii()) access.requireScope("export:pii");
        var csv = renderCsv(rows, masked); var bytes = csv.getBytes(StandardCharsets.UTF_8); var checksum = uploadSecurity.checksum(bytes); var id = UUID.randomUUID(); var expires = Instant.now(clock).plus(config.downloadTtl());
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString((id + ":" + expires.toEpochMilli() + ":" + checksum).getBytes(StandardCharsets.UTF_8)); var audit = new ExportAuditEntity(); audit.id = id; audit.organizationId = access.organizationId(); audit.unitId = request.unitId(); audit.operation = "EXPORT"; audit.purpose = request.purpose(); audit.format = request.format(); audit.checksum = checksum; audit.piiMasked = masked; audit.expiresAt = expires; audit.actorId = access.actorId(); audit.correlationId = access.correlationId(); audit.metadataJson = json(Map.of("rows", rows.size(), "purpose", request.purpose())); audit.artifactContent = csv; audit.createdAt = Instant.now(clock); audits.persist(audit);
        return new ExportResult(id, request.purpose(), request.format(), "/internal/import-export/exports/" + id + "/download?token=" + token, checksum, expires, masked);
    }

    public String download(UUID id, String token, AccessContext access) {
        access.requireScope("export:read"); var audit = audits.find("id = ?1 and organizationId = ?2", id, access.organizationId()).firstResultOptional().orElseThrow(() -> ImportExportException.notFound("export not found in organization scope"));
        if (audit.expiresAt.isBefore(Instant.now(clock))) throw ImportExportException.conflict("export download has expired");
        var expected = Base64.getUrlEncoder().withoutPadding().encodeToString((id + ":" + audit.expiresAt.toEpochMilli() + ":" + audit.checksum).getBytes(StandardCharsets.UTF_8));
        if (token == null || !java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), token.getBytes(StandardCharsets.US_ASCII))) throw ImportExportException.unauthorized("download token is invalid");
        return audit.artifactContent;
    }

    private ImportBatchEntity scoped(UUID id, AccessContext access) { return batches.scoped(id, access.organizationId()).orElseThrow(() -> ImportExportException.notFound("batch not found in organization scope")); }
    private BatchResponse response(ImportBatchEntity e) { var preview = e.previewJson == null ? null : readPreview(e.previewJson, e); return new BatchResponse(e.id, e.organizationId, e.unitId, e.manifestId, e.manifestVersion, e.purpose, e.status, e.dryRun, e.checksum, e.rowsSeen, e.validRows, e.invalidRows, e.conflicts, e.checkpointRow + ":" + e.checksum, e.createdAt, e.updatedAt, preview); }
    private BatchPreview readPreview(String value, ImportBatchEntity entity) { try { var issues = mapper.readValue(value, new TypeReference<List<RowIssue>>() {}); return new BatchPreview(entity.rowsSeen, entity.validRows, entity.invalidRows, entity.conflicts, issues, entity.checksum); } catch (Exception e) { return new BatchPreview(entity.rowsSeen, entity.validRows, entity.invalidRows, entity.conflicts, List.of(), entity.checksum); } }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); } }
    private static String contentType(String format) { return format.equals("CSV") ? "text/csv" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; }
    private static boolean requiresUnit(String purpose) { var p = purpose.toLowerCase(Locale.ROOT); return p.contains("unit") || p.contains("stock") || p.contains("appointment") || p.contains("sale"); }
    private static String renderCsv(List<Map<String, String>> rows, boolean mask) { if (rows.isEmpty()) return ""; var headers = new ArrayList<>(rows.getFirst().keySet()); var out = new StringBuilder(String.join(",", headers)).append('\n'); for (var row : rows) { for (var i = 0; i < headers.size(); i++) { if (i > 0) out.append(','); var value = row.getOrDefault(headers.get(i), ""); out.append(escape(mask ? mask(headers.get(i), value) : value)); } out.append('\n'); } return out.toString(); }
    private static String mask(String key, String value) { var k = key.toLowerCase(Locale.ROOT); if (k.contains("email")) { var at = value.indexOf('@'); return at > 1 ? value.charAt(0) + "***" + value.substring(at) : "***"; } if (k.contains("phone") || k.contains("cpf") || k.contains("cnpj")) return value.length() < 4 ? "***" : "***" + value.substring(value.length() - 2); return value; }
    private static String escape(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
