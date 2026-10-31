package com.bankease.controller;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.ApplyLoanRequest;
import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.dto.RepayLoanRequest;
import com.bankease.entity.AccountType;
import com.bankease.entity.LoanType;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoanControllerTest {

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
        String email = "loantester." + System.currentTimeMillis() + "@bankease.com";
        AuthResponse auth = authService.register(new RegisterRequest("Loan Customer", email, "password123", "5544332211"));
        userToken = auth.getToken();

        AccountResponse acc = accountService.createAccount(
                email,
                new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("1000.00"), "INR")
        );
        accountNumber = acc.getAccountNumber();
    }

    @Test
    @DisplayName("Should apply, disburse loan to account, and repay installment")
    void testApplyAndRepayLoan() throws Exception {
        ApplyLoanRequest applyReq = new ApplyLoanRequest(
                accountNumber,
                LoanType.PERSONAL,
                new BigDecimal("20000.00"),
                12
        );

        MvcResult applyResult = mockMvc.perform(post("/api/loans/apply")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanNumber", startsWith("LOAN-")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.principalAmount", is(20000.00)))
                .andExpect(jsonPath("$.monthlyInstallment", notNullValue()))
                .andReturn();

        Long loanId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify account balance was credited with principal (1000 + 20000 = 21000)
        BigDecimal balanceAfterDisbursal = accountRepository.findByAccountNumber(accountNumber).get().getBalance();
        assertEquals(new BigDecimal("21000.00"), balanceAfterDisbursal);

        // Repay installment of 5000
        RepayLoanRequest repayReq = new RepayLoanRequest(new BigDecimal("5000.00"));
        mockMvc.perform(post("/api/loans/" + loanId + "/repay")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repayReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Account balance after repayment: 21000 - 5000 = 16000
        BigDecimal balanceAfterRepay = accountRepository.findByAccountNumber(accountNumber).get().getBalance();
        assertEquals(new BigDecimal("16000.00"), balanceAfterRepay);

        // List loans
        mockMvc.perform(get("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
