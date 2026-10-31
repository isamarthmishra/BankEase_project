package com.bankease.controller;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.dto.TransferRequest;
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
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String user1Token;
    private String user2Token;
    private String acc1Number;
    private String acc2Number;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        String email1 = "txntester1." + ts + "@bankease.com";
        AuthResponse a1 = authService.register(new RegisterRequest("Txn User 1", email1, "password123", "1212121212"));
        user1Token = a1.getToken();

        String email2 = "txntester2." + ts + "@bankease.com";
        AuthResponse a2 = authService.register(new RegisterRequest("Txn User 2", email2, "password123", "3434343434"));
        user2Token = a2.getToken();

        AccountResponse r1 = accountService.createAccount(
                email1,
                new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("8000.00"), "INR")
        );
        acc1Number = r1.getAccountNumber();

        AccountResponse r2 = accountService.createAccount(
                email2,
                new CreateAccountRequest(AccountType.CHECKING, new BigDecimal("1000.00"), "INR")
        );
        acc2Number = r2.getAccountNumber();
    }

    @Test
    @DisplayName("Should retrieve transaction history and transaction receipt")
    void testTransactionHistoryAndReceipt() throws Exception {
        // Execute a transfer from acc1 to acc2
        TransferRequest transferRequest = new TransferRequest(
                acc1Number,
                acc2Number,
                new BigDecimal("1200.00"),
                "Invoice payment #42",
                null
        );

        MvcResult transferResult = mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refNumber = objectMapper.readTree(transferResult.getResponse().getContentAsString()).get("referenceNumber").asText();

        // 1. Retrieve transaction history for account
        mockMvc.perform(get("/api/transactions/account/" + acc1Number)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].referenceNumber", is(refNumber)))
                .andExpect(jsonPath("$[0].amount", is(1200.00)))
                .andExpect(jsonPath("$[0].status", is("SUCCESS")));

        // 2. Retrieve receipt by referenceNumber
        mockMvc.perform(get("/api/transactions/receipt/" + refNumber)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber", is(refNumber)))
                .andExpect(jsonPath("$.amount", is(1200.00)))
                .andExpect(jsonPath("$.balanceAfter", is(6800.00)))
                .andExpect(jsonPath("$.description", is("Invoice payment #42")));

        // 3. User 2 (the recipient) can also view receipt
        mockMvc.perform(get("/api/transactions/receipt/" + refNumber)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber", is(refNumber)));
    }
}
