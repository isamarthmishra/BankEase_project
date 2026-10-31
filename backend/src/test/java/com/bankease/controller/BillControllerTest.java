package com.bankease.controller;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.CreateBillRequest;
import com.bankease.dto.PayBillRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.entity.AccountType;
import com.bankease.entity.BillCategory;
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
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillControllerTest {

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
        String email = "billtester." + System.currentTimeMillis() + "@bankease.com";
        AuthResponse auth = authService.register(new RegisterRequest("Bill User", email, "password123", "7788990011"));
        userToken = auth.getToken();

        AccountResponse acc = accountService.createAccount(
                email,
                new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("3000.00"), "INR")
        );
        accountNumber = acc.getAccountNumber();
    }

    @Test
    @DisplayName("Should create and pay a utility bill, updating account balance and status")
    void testCreateAndPayBill() throws Exception {
        CreateBillRequest createReq = new CreateBillRequest(
                "Electricity Board (BESCOM)",
                BillCategory.ELECTRICITY,
                "BES-987654",
                new BigDecimal("850.00"),
                LocalDate.now().plusDays(10)
        );

        MvcResult createResult = mockMvc.perform(post("/api/bills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.amount", is(850.00)))
                .andReturn();

        Long billId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Pay bill
        PayBillRequest payReq = new PayBillRequest(accountNumber);
        mockMvc.perform(post("/api/bills/" + billId + "/pay")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.transactionReference", startsWith("BILL-")))
                .andExpect(jsonPath("$.paidFromAccount", is(accountNumber)));

        // Verify balance debited: 3000 - 850 = 2150
        BigDecimal newBalance = accountRepository.findByAccountNumber(accountNumber).get().getBalance();
        assertEquals(new BigDecimal("2150.00"), newBalance);

        // Verify paying already paid bill fails
        mockMvc.perform(post("/api/bills/" + billId + "/pay")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already been paid")));
    }
}
