package com.bankease.controller;

import com.bankease.dto.AuthResponse;
import com.bankease.dto.BeneficiaryRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userToken;

    @BeforeEach
    void setUp() {
        AuthResponse auth = authService.register(new RegisterRequest(
                "Beneficiary Tester",
                "ben.test." + System.currentTimeMillis() + "@bankease.com",
                "password123",
                "9988776655"
        ));
        userToken = auth.getToken();
    }

    @Test
    @DisplayName("Should add, list, and delete beneficiary")
    void testBeneficiaryLifecycle() throws Exception {
        BeneficiaryRequest request = new BeneficiaryRequest(
                "Jane Doe",
                "100200300400",
                "BankEase",
                "BANKEASE001",
                "Jane Savings"
        );

        // Add
        MvcResult addResult = mockMvc.perform(post("/api/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.accountNumber", is("100200300400")))
                .andReturn();

        Long beneficiaryId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asLong();

        // List
        mockMvc.perform(get("/api/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountNumber", is("100200300400")));

        // Delete
        mockMvc.perform(delete("/api/beneficiaries/" + beneficiaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());

        // Verify list is now empty
        mockMvc.perform(get("/api/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
