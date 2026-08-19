package com.egoistian.flowops.audit;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditEventWriter {
    private final JdbcClient jdbc;

    public AuditEventWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void append(
            UUID organizationId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            UUID actorId) {
        jdbc.sql("""
                        insert into audit_events (
                            id, organization_id, aggregate_type, aggregate_id,
                            event_type, actor_id, payload, created_at
                        ) values (
                            :id, :organizationId, :aggregateType, :aggregateId,
                            :eventType, :actorId, '{}'::jsonb, current_timestamp
                        )
                        """)
                .param("id", UUID.randomUUID())
                .param("organizationId", organizationId)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .param("eventType", eventType)
                .param("actorId", actorId)
                .update();
    }
}
