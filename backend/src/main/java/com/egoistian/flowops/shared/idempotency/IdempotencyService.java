package com.egoistian.flowops.shared.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public IdempotencyService(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(
            UUID organizationId,
            String operation,
            String idempotencyKey,
            String requestHash,
            Class<T> responseType,
            Supplier<T> operationBody) {
        Optional<StoredResult> existing = find(
                organizationId, operation, idempotencyKey);
        if (existing.isPresent()) {
            StoredResult stored = existing.orElseThrow();
            if (!stored.requestHash().equals(requestHash)) {
                throw new IdempotencyKeyReused();
            }
            if (!"SUCCEEDED".equals(stored.status())) {
                throw new IdempotencyInProgress();
            }
            return read(stored.responseJson(), responseType);
        }

        UUID recordId = UUID.randomUUID();
        jdbc.sql("""
                        insert into idempotency_records (
                            id, organization_id, operation, idempotency_key,
                            request_hash, status, created_at, updated_at
                        ) values (
                            :id, :organizationId, :operation, :idempotencyKey,
                            :requestHash, 'PROCESSING', current_timestamp, current_timestamp
                        )
                        """)
                .param("id", recordId)
                .param("organizationId", organizationId)
                .param("operation", operation)
                .param("idempotencyKey", idempotencyKey)
                .param("requestHash", requestHash)
                .update();

        T response = operationBody.get();
        String responseJson = write(response);
        jdbc.sql("""
                        update idempotency_records
                        set status = 'SUCCEEDED',
                            response_json = cast(:responseJson as jsonb),
                            updated_at = current_timestamp
                        where id = :id
                        """)
                .param("responseJson", responseJson)
                .param("id", recordId)
                .update();
        return response;
    }

    private Optional<StoredResult> find(
            UUID organizationId, String operation, String idempotencyKey) {
        return jdbc.sql("""
                        select request_hash, status, response_json::text as response_json
                        from idempotency_records
                        where organization_id = :organizationId
                          and operation = :operation
                          and idempotency_key = :idempotencyKey
                        """)
                .param("organizationId", organizationId)
                .param("operation", operation)
                .param("idempotencyKey", idempotencyKey)
                .query((rs, rowNum) -> new StoredResult(
                        rs.getString("request_hash"),
                        rs.getString("status"),
                        rs.getString("response_json")))
                .optional();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist idempotency response", exception);
        }
    }

    private <T> T read(String value, Class<T> responseType) {
        try {
            return objectMapper.readValue(value, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read idempotency response", exception);
        }
    }

    private record StoredResult(String requestHash, String status, String responseJson) {
    }
}
