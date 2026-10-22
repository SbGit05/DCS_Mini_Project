package com.distributed.banking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Scenario 18: End-to-End REST API Flow for Customer and Administrator")
    void testEndToEndApiFlow() throws Exception {
        // 1. Customer Login
        String customerLoginJson = objectMapper.writeValueAsString(Map.of(
                "username", "shreedhar",
                "password", "user123"
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"))
                .andReturn();

        Map<?, ?> customerLoginData = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        String customerToken = "Bearer " + customerLoginData.get("token");

        // 2. Fetch Customer Account
        mockMvc.perform(get("/api/customer/account")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].accountNumber").value("PUN-1001"));

        // 3. Deposit via API
        String depositPayload = objectMapper.writeValueAsString(Map.of(
                "accountNumber", "PUN-1001",
                "amount", 500.0,
                "idempotencyKey", "API-DEP-" + UUID.randomUUID()
        ));

        mockMvc.perform(post("/api/customer/deposit")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 4. Withdrawal via API
        String withdrawPayload = objectMapper.writeValueAsString(Map.of(
                "accountNumber", "PUN-1001",
                "amount", 200.0,
                "idempotencyKey", "API-WTH-" + UUID.randomUUID()
        ));

        mockMvc.perform(post("/api/customer/withdraw")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Cross-Branch Transfer via 2PC API
        String transferPayload = objectMapper.writeValueAsString(Map.of(
                "fromAccount", "PUN-1001",
                "toAccount", "MUM-2001",
                "amount", 300.0,
                "idempotencyKey", "API-XFER-" + UUID.randomUUID()
        ));

        mockMvc.perform(post("/api/customer/transfer")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER_DISTRIBUTED"));

        // 6. Administrator Login
        String adminLoginJson = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "admin123"
        ));

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andReturn();

        Map<?, ?> adminData = objectMapper.readValue(adminLoginResult.getResponse().getContentAsString(), Map.class);
        String adminToken = "Bearer " + adminData.get("token");

        // 7. Admin Dashboard Query
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBranches").value(3))
                .andExpect(jsonPath("$.currentLamportClock").isNumber())
                .andExpect(jsonPath("$.twoPhaseTransactions").isArray());

        // 8. Distributed Node Failure & Bully Election Trigger via API
        mockMvc.perform(post("/api/distributed/election/start?initiatorId=1")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electedLeaderId").isNumber());

        // 9. Unauthorized Role Access Check: Customer trying to access Admin Dashboard
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 19: Direct Election API Call Rejected When Initiator is Stopped")
    void testRejectElectionWhenInitiatorUnavailable() throws Exception {
        // Admin Login
        String adminLoginJson = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "admin123"
        ));
        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginJson))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> adminData = objectMapper.readValue(adminLoginResult.getResponse().getContentAsString(), Map.class);
        String adminToken = "Bearer " + adminData.get("token");

        try {
            // Stop Node 3 via API
            mockMvc.perform(post("/api/distributed/node/3/stop")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("STOPPED"));

            // Attempt election from stopped Node 3 via API
            mockMvc.perform(post("/api/distributed/election/start?initiatorId=3")
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error", containsString("Election cannot be started: Node 3 is STOPPED")));

            // Direct election from running Node 2 must succeed and elect Node 2
            mockMvc.perform(post("/api/distributed/election/start?initiatorId=2")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.electedLeaderId").value(2));

        } finally {
            // Restore Node 3 to RUNNING
            mockMvc.perform(post("/api/distributed/node/3/start")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());
        }
    }
}
