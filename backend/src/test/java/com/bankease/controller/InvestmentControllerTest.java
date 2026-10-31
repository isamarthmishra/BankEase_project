package com.bankease.controller;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.CreateInvestmentRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.entity.AccountType;
import com.bankease.entity.InvestmentType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String userToken;
    private String accountNumber;

    @BeforeEach
    void setUp() {
        String email = "invtester." + System.currentTimeMillis() + "@bankease.com";
        AuthResponse auth = authService.register(new RegisterRequest("Investor", email, "password123", "9900112233"));
        userToken = auth.getToken();

        AccountResponse acc = accountService.createAccount(
                email,
                new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("10000.00"), "INR")
        );
        accountNumber = acc.getAccountNumber();
    }

    @Test
    @DisplayName("Should create fixed deposit investment and withdraw with returns")
    void testCreateAndWithdrawInvestment() throws Exception {
        CreateInvestmentRequest createReq = new CreateInvestmentRequest(
                accountNumber,
                InvestmentType.FIXED_DEPOSIT,
                new BigDecimal("5000.00"),
                12
        );

        MvcResult createResult = mockMvc.perform(post("/api/investments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.investmentNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.investedAmount", is(5000.00)))
                .andExpect(jsonPath("$.expectedMaturityAmount", greaterThan(5000.0)))
                .andReturn();

        Long invId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Account debited: 10000 - 5000 = 5000
        BigDecimal balAfterInvest = accountRepository.findByAccountNumber(accountNumber).get().getBalance();
        assertEquals(new BigDecimal("5000.00"), balAfterInvest);

        // Withdraw / liquidate investment
        mockMvc.perform(post("/api/investments/" + invId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WITHDRAWN")));

        // Account credited back with maturity amount: 5000 + 5362.50 = 10362.50
        BigDecimal balAfterWithdraw = accountRepository.findByAccountNumber(accountNumber).get().getBalance();
        assertEquals(new BigDecimal("10362.50"), balAfterWithdraw);

        // List investments
        mockMvc.perform(get("/api/investments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
