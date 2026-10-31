package com.bankease.controller;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.dto.TransferRequest;
import com.bankease.entity.AccountType;
import com.bankease.repository.AccountRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String user1Email;
    private String user1Token;
    private String user2Email;
    private String user2Token;

    private String account1Number;
    private String account2Number;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        user1Email = "user1." + ts + "@bankease.com";
        AuthResponse user1 = authService.register(new RegisterRequest("User One", user1Email, "password123", "1111111111"));
        user1Token = user1.getToken();

        user2Email = "user2." + ts + "@bankease.com";
        AuthResponse user2 = authService.register(new RegisterRequest("User Two", user2Email, "password123", "2222222222"));
        user2Token = user2.getToken();

        // Account 1: 10000.00
        AccountResponse acc1 = accountService.createAccount(
                user1Email,
                new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("10000.00"), "INR")
        );
        account1Number = acc1.getAccountNumber();

        // Account 2: 2000.00
        AccountResponse acc2 = accountService.createAccount(
                user2Email,
                new CreateAccountRequest(AccountType.CHECKING, new BigDecimal("2000.00"), "INR")
        );
        account2Number = acc2.getAccountNumber();
    }

    @Test
    @DisplayName("Should execute valid fund transfer between two accounts successfully")
    void testSuccessfulTransfer() throws Exception {
        TransferRequest request = new TransferRequest(
                account1Number,
                account2Number,
                new BigDecimal("3500.00"),
                "Payment for services",
                "IDEMP-" + System.currentTimeMillis()
        );

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber", startsWith("TXN-")))
                .andExpect(jsonPath("$.sourceAccountNumber", is(account1Number)))
                .andExpect(jsonPath("$.targetAccountNumber", is(account2Number)))
                .andExpect(jsonPath("$.amount", is(3500.00)))
                .andExpect(jsonPath("$.balanceAfter", is(6500.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // Verify database balances directly
        BigDecimal bal1 = accountRepository.findByAccountNumber(account1Number).get().getBalance();
        BigDecimal bal2 = accountRepository.findByAccountNumber(account2Number).get().getBalance();
        assertEquals(new BigDecimal("6500.00"), bal1);
        assertEquals(new BigDecimal("5500.00"), bal2);
    }

    @Test
    @DisplayName("Should reject transfer when account has insufficient balance")
    void testInsufficientBalance() throws Exception {
        TransferRequest request = new TransferRequest(
                account1Number,
                account2Number,
                new BigDecimal("15000.00"), // only 10000 available
                "Overdraft attempt",
                null
        );

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Insufficient Funds")));

        // Verify balances unchanged
        BigDecimal bal1 = accountRepository.findByAccountNumber(account1Number).get().getBalance();
        BigDecimal bal2 = accountRepository.findByAccountNumber(account2Number).get().getBalance();
        assertEquals(new BigDecimal("10000.00"), bal1);
        assertEquals(new BigDecimal("2000.00"), bal2);
    }

    @Test
    @DisplayName("Should reject transfer between identical accounts")
    void testSameAccountTransfer() throws Exception {
        TransferRequest request = new TransferRequest(
                account1Number,
                account1Number,
                new BigDecimal("500.00"),
                "Self transfer",
                null
        );

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be the same")));
    }

    @Test
    @DisplayName("Should reject transfer if user attempts to transfer from an account they do not own")
    void testUnauthorizedTransfer() throws Exception {
        // User 2 trying to transfer from User 1's account
        TransferRequest request = new TransferRequest(
                account1Number,
                account2Number,
                new BigDecimal("100.00"),
                "Unauthorized theft attempt",
                null
        );

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("Should prevent double processing when idempotency key is repeated")
    void testIdempotencyPreventsDuplicateTransfer() throws Exception {
        String idempotencyKey = "DUP-CHECK-" + System.currentTimeMillis();
        TransferRequest request = new TransferRequest(
                account1Number,
                account2Number,
                new BigDecimal("1000.00"),
                "Idempotent Payment",
                idempotencyKey
        );

        // First transfer
        MvcResult firstResult = mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String refNumber1 = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("referenceNumber").asText();

        // Second transfer with same idempotency key
        MvcResult secondResult = mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String refNumber2 = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("referenceNumber").asText();

        // Both calls should return the same reference number
        assertEquals(refNumber1, refNumber2);

        // Verify balance was only deducted once: 10000 - 1000 = 9000 (NOT 8000!)
        BigDecimal bal1 = accountRepository.findByAccountNumber(account1Number).get().getBalance();
        assertEquals(new BigDecimal("9000.00"), bal1);
    }
}
