package com.bankease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RepayLoanRequest {

    @NotNull(message = "Repayment amount is required")
    @DecimalMin(value = "0.01", message = "Repayment amount must be at least 0.01")
    private BigDecimal amount;

    public RepayLoanRequest() {
    }

    public RepayLoanRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
