package com.thebestpetshop.importexport.repository;

import com.thebestpetshop.importexport.model.entity.ImportBatchEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ImportBatchRepository implements PanacheRepository<ImportBatchEntity> {
    public Optional<ImportBatchEntity> byIdempotency(UUID organizationId, String key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }
    public Optional<ImportBatchEntity> scoped(UUID id, UUID organizationId) {
        return find("id = ?1 and organizationId = ?2", id, organizationId).firstResultOptional();
    }
}
