package com.bankease.controller;

import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.DepositRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.entity.AccountType;
import com.bankease.service.AccountService;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        AuthResponse userAuth = authService.register(new RegisterRequest(
                "Account Owner",
                "owner." + System.currentTimeMillis() + "@bankease.com",
                "password123",
                "1112223334"
        ));
        userToken = userAuth.getToken();

        AuthResponse otherAuth = authService.register(new RegisterRequest(
                "Other Person",
                "other." + System.currentTimeMillis() + "@bankease.com",
                "password123",
                "9998887776"
        ));
        otherUserToken = otherAuth.getToken();
    }

    @Test
    @DisplayName("Should create account with initial deposit and return details")
    void testCreateAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                "INR"
        );

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber", not(emptyOrNullString())))
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andExpect(jsonPath("$.balance", is(5000.00)))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Should fetch balance for owned account")
    void testGetBalance() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.CHECKING, new BigDecimal("2500.50"), "INR");
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        String accountNumber = objectMapper.readTree(responseStr).get("accountNumber").asText();

        mockMvc.perform(get("/api/accounts/" + accountNumber + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is(accountNumber)))
                .andExpect(jsonPath("$.balance", is(2500.50)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Should reject balance inquiry if account belongs to another user (403 Forbidden)")
    void testUnauthorizedAccountAccess() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("1000.00"), "INR");
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountNumber = objectMapper.readTree(result.getResponse().getContentAsString()).get("accountNumber").asText();

        // Access with other user's token
        mockMvc.perform(get("/api/accounts/" + accountNumber + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("Should deposit funds successfully into account")
    void testDepositFunds() throws Exception {
        CreateAccountRequest createReq = new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("100.00"), "INR");
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountNumber = objectMapper.readTree(result.getResponse().getContentAsString()).get("accountNumber").asText();

        DepositRequest depositReq = new DepositRequest(new BigDecimal("450.00"), "Salary credit");
        mockMvc.perform(post("/api/accounts/" + accountNumber + "/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(450.00)))
                .andExpect(jsonPath("$.balanceAfter", is(550.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }
}
