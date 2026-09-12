package com.thebestpetshop.importexport.repository;

import com.thebestpetshop.importexport.model.entity.ExportAuditEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExportAuditRepository implements PanacheRepository<ExportAuditEntity> {}
