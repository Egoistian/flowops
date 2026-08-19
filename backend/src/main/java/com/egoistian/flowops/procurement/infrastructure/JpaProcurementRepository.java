package com.egoistian.flowops.procurement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface JpaProcurementRepository extends JpaRepository<JpaProcurementRequestEntity, UUID> {
    Optional<JpaProcurementRequestEntity> findByIdAndOrganizationId(
            UUID id, UUID organizationId);
}
