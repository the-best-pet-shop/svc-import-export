package com.thebestpetshop.importexport.model.entity;

import com.thebestpetshop.importexport.model.BatchStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_export_batches", uniqueConstraints = @UniqueConstraint(name = "uk_import_batch_idempotency", columnNames = {"organization_id", "idempotency_key"}))
public class ImportBatchEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "organization_id", nullable = false) public UUID organizationId;
    @Column(name = "unit_id") public UUID unitId;
    @Column(name = "manifest_id", nullable = false) public String manifestId;
    @Column(name = "manifest_version", nullable = false) public int manifestVersion;
    @Column(nullable = false) public String purpose;
    @Column(nullable = false) public String format;
    @Column(nullable = false) public String encoding;
    @Column(nullable = false) public String timezone;
    @Column(name = "scope_policy", nullable = false) public String scopePolicy;
    @Column(nullable = false) public String checksum;
    @Column(name = "idempotency_key", nullable = false) public String idempotencyKey;
    @Column(name = "source_file", nullable = false) public String sourceFile;
    @Column(name = "content_type", nullable = false) public String contentType;
    @Lob @Column(name = "source_content", nullable = false) public String sourceContent;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public BatchStatus status;
    @Column(name = "dry_run", nullable = false) public boolean dryRun;
    @Column(name = "approval_reason") public String approvalReason;
    @Column(name = "created_by", nullable = false) public UUID createdBy;
    @Column(name = "checkpoint_row", nullable = false) public long checkpointRow;
    @Column(name = "rows_seen", nullable = false) public long rowsSeen;
    @Column(name = "valid_rows", nullable = false) public long validRows;
    @Column(name = "invalid_rows", nullable = false) public long invalidRows;
    @Column(nullable = false) public long conflicts;
    @Lob @Column(name = "preview_json") public String previewJson;
    @Lob @Column(name = "manifest_json", nullable = false) public String manifestJson;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "approved_at") public Instant approvedAt;
    @Column(name = "rolled_back_at") public Instant rolledBackAt;
}
