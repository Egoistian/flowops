package com.egoistian.flowops.procurement.api;

import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Sql(scripts = "/db/testdata/demo_accounts.sql")
@Sql(statements = {
        "delete from audit_events",
        "delete from idempotency_records",
        "delete from approval_steps",
        "delete from procurement_items",
        "delete from procurement_requests",
        "delete from user_roles",
        "delete from users",
        "delete from organizations"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ProcurementApiTest extends PostgresIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void createsAndReadsADraftUsingTheAuthenticatedOrganizationAndRequester() throws Exception {
        MockHttpSession session = login(
                "northstar", "requester@northstar.example.com", "demo-password");

        MvcResult create = mockMvc.perform(post("/api/procurement/requests")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "개발용 장비",
                                  "purpose": "통합 테스트 환경",
                                  "budgetCode": "ENG-2026",
                                  "items": [
                                    {"name": "노트북", "quantity": 2, "unitPriceKrw": 1200000},
                                    {"name": "도킹 스테이션", "quantity": 1, "unitPriceKrw": 180000}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/procurement/requests/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmountKrw").value(2_580_000))
                .andReturn();

        String location = create.getResponse().getHeader("Location");
        mockMvc.perform(get(location).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("개발용 장비"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmountKrw").value(2_580_000))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0));
    }

    private MockHttpSession login(
            String organizationKey, String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationKey": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(organizationKey, email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
