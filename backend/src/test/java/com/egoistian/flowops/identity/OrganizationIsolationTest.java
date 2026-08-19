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
        "delete from user_roles",
        "delete from users",
        "delete from organizations"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrganizationIsolationTest extends PostgresIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void returnsTheCurrentOrganizationAndHidesAnotherOrganization() throws Exception {
        MockHttpSession northstarSession = loginAsNorthstarRequester();

        mockMvc.perform(get("/api/organizations/11111111-1111-1111-1111-111111111111")
                        .session(northstarSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Northstar Studio"));

        mockMvc.perform(get("/api/organizations/22222222-2222-2222-2222-222222222222")
                        .session(northstarSession))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Acme Operations"))));
    }

    private MockHttpSession loginAsNorthstarRequester() throws Exception {
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
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
