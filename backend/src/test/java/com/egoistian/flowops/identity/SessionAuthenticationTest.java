package com.egoistian.flowops.identity;

import com.egoistian.flowops.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

@AutoConfigureMockMvc
@Sql(scripts = "/db/testdata/demo_accounts.sql")
@Sql(statements = {
        "delete from user_roles",
        "delete from users",
        "delete from organizations"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SessionAuthenticationTest extends PostgresIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void logsInWithOrganizationKeyAndReturnsTheScopedSession() throws Exception {
        mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationKey": "northstar",
                                  "email": "requester@northstar.example.com",
                                  "password": "demo-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("11111111-1111-1111-1111-111111111101"))
                .andExpect(jsonPath("$.organizationId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.roles[0]").value("REQUESTER"));
    }

    @Test
    void returnsAGenericUnauthorizedProblemForAnInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationKey": "northstar",
                                  "email": "requester@northstar.example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(content().string(not(containsString("requester@northstar.example.com"))));
    }

    @Test
    void returnsTheAuthenticatedSessionWithoutExposingCredentials() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationKey": "northstar",
                                  "email": "requester@northstar.example.com",
                                  "password": "demo-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Min Park"))
                .andExpect(jsonPath("$.roles[0]").value("REQUESTER"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("requester@northstar.example.com"))));
    }

    @Test
    void rejectsLoginWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/api/session/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationKey": "northstar",
                                  "email": "requester@northstar.example.com",
                                  "password": "demo-password"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsSessionLookupWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
