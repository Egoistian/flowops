package com.egoistian.flowops.organization;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OrganizationRepository {
    private final JdbcClient jdbc;

    public OrganizationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OrganizationSummary> findScoped(UUID scopeOrganizationId, UUID requestedOrganizationId) {
        return jdbc.sql("""
                        select id, organization_key, name, status
                        from organizations
                        where id = :requestedOrganizationId
                          and id = :scopeOrganizationId
                        """)
                .param("requestedOrganizationId", requestedOrganizationId)
                .param("scopeOrganizationId", scopeOrganizationId)
                .query((rs, rowNum) -> new OrganizationSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("organization_key"),
                        rs.getString("name"),
                        rs.getString("status")))
                .optional();
    }

    public record OrganizationSummary(UUID id, String key, String name, String status) {
    }
}
