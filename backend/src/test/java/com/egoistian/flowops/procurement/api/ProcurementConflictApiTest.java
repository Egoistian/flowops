package com.egoistian.flowops.procurement.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class ProcurementConflictApiTest extends PostgresIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void returnsValidationProblemForAnInvalidItemQuantity() throws Exception {
        MockHttpSession session = login("northstar", "requester@northstar.example.com");

        mockMvc.perform(post("/api/procurement/requests")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "개발용 장비",
                                  "purpose": "통합 테스트 환경",
                                  "budgetCode": "ENG-2026",
                                  "items": [
                                    {"name": "노트북", "quantity": 0, "unitPriceKrw": 1200000}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"));
    }

    @Test
    void returnsValidationProblemWhenItemsAreMissing() throws Exception {
        MockHttpSession session = login("northstar", "requester@northstar.example.com");

        mockMvc.perform(post("/api/procurement/requests")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "개발용 장비",
                                  "purpose": "통합 테스트 환경",
                                  "budgetCode": "ENG-2026"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items"));
    }

    @Test
    void hidesARequestFromAnotherOrganization() throws Exception {
        MockHttpSession northstar = login("northstar", "requester@northstar.example.com");
        String location = createDraft(northstar, 900_000).getResponse().getHeader("Location");
        MockHttpSession acme = login("acme", "requester@acme.example.com");

        mockMvc.perform(get(location).session(acme))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCUREMENT_NOT_FOUND"))
                .andExpect(content().string(not(containsString("개발용 장비"))));
    }

    @Test
    void rejectsAnIdempotencyKeyReusedWithDifferentContent() throws Exception {
        MockHttpSession requester = login("northstar", "requester@northstar.example.com");
        String location = createDraft(requester, 900_000).getResponse().getHeader("Location");

        mockMvc.perform(post(location + "/submit")
                        .session(requester)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        String secondLocation = createDraft(requester, 180_000)
                .getResponse().getHeader("Location");

        mockMvc.perform(post(secondLocation + "/submit")
                        .session(requester)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void returnsConflictWhenApprovalUsesAStaleVersion() throws Exception {
        MockHttpSession requester = login("northstar", "requester@northstar.example.com");
        String location = createDraft(requester, 900_000).getResponse().getHeader("Location");
        MvcResult submit = mockMvc.perform(post(location + "/submit")
                        .session(requester)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody()))
                .andExpect(status().isOk())
                .andReturn();
        long submittedVersion = objectMapper.readTree(
                submit.getResponse().getContentAsString()).get("version").asLong();
        MockHttpSession reviewer = login("northstar", "reviewer@northstar.example.com");

        mockMvc.perform(post(location + "/approve")
                        .session(reviewer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\": %d}".formatted(submittedVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post(location + "/approve")
                        .session(reviewer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\": %d}".formatted(submittedVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private MockHttpSession login(String organizationKey, String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationKey": "%s",
                                  "email": "%s",
                                  "password": "demo-password"
                                }
                                """.formatted(organizationKey, email)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private MvcResult createDraft(MockHttpSession session, long unitPriceKrw) throws Exception {
        return mockMvc.perform(post("/api/procurement/requests")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "개발용 장비",
                                  "purpose": "통합 테스트 환경",
                                  "budgetCode": "ENG-2026",
                                  "items": [
                                    {"name": "노트북", "quantity": 1, "unitPriceKrw": %d}
                                  ]
                                }
                                """.formatted(unitPriceKrw)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String submitBody() {
        JsonNode body = objectMapper.createObjectNode()
                .put("idempotencyKey", "submit-api-001");
        return body.toString();
    }
}
