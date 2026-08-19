package com.egoistian.flowops;

import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest extends PostgresIntegrationTest {
    @Autowired
    JdbcClient jdbc;

    @Test
    void createsTheProcurementSchemaFromAnEmptyDatabase() {
        Integer count = jdbc.sql("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'organizations',
                    'users',
                    'procurement_requests',
                    'audit_events'
                  )
                """).query(Integer.class).single();

        assertThat(count).isEqualTo(4);
    }
}
