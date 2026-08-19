package com.egoistian.flowops;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowOpsApplicationTest {
    @Test
    void exposesTheApplicationClass() {
        assertThat(FlowOpsApplication.class).isNotNull();
    }
}
