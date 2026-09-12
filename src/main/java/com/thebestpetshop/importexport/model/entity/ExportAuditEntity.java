package com.thebestpetshop.importexport.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_export_audits")
public class ExportAuditEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "organization_id", nullable = false) public UUID organizationId;
    @Column(name = "unit_id") public UUID unitId;
    @Column(nullable = false) public String operation;
    @Column(nullable = false) public String purpose;
    @Column(nullable = false) public String format;
    @Column(nullable = false) public String checksum;
    @Column(name = "pii_masked", nullable = false) public boolean piiMasked;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
    @Column(name = "actor_id", nullable = false) public UUID actorId;
    @Column(name = "correlation_id", nullable = false) public UUID correlationId;
    @Lob @Column(name = "metadata_json", nullable = false) public String metadataJson;
    @Lob @Column(name = "artifact_content", nullable = false) public String artifactContent;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
