package com.egoistian.flowops.procurement.infrastructure;

import com.egoistian.flowops.procurement.domain.ProcurementRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ProcurementStore {
    private final JpaProcurementRepository repository;

    public ProcurementStore(JpaProcurementRepository repository) {
        this.repository = repository;
    }

    public Optional<ProcurementRequest> find(UUID organizationId, UUID requestId) {
        return repository.findByIdAndOrganizationId(requestId, organizationId)
                .map(JpaProcurementRequestEntity::toDomain);
    }

    public ProcurementRequest save(ProcurementRequest request) {
        JpaProcurementRequestEntity entity;
        if (request.persisted()) {
            entity = repository.findByIdAndOrganizationId(
                            request.id(), request.organizationId())
                    .orElseThrow(() -> new IllegalStateException(
                            "persisted request is missing"));
            entity.apply(request);
        } else {
            entity = JpaProcurementRequestEntity.from(request);
        }
        JpaProcurementRequestEntity saved = repository.saveAndFlush(entity);
        return saved.toDomain();
    }
}
