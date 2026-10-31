package com.bankease.dto;

import com.bankease.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotNull(message = "Account type is required (SAVINGS, CHECKING, CURRENT, FIXED_DEPOSIT)")
    private AccountType accountType;

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial deposit must not be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    private String currency = "INR";

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(AccountType accountType, BigDecimal initialDeposit, String currency) {
        this.accountType = accountType;
        this.initialDeposit = initialDeposit != null ? initialDeposit : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "INR";
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
