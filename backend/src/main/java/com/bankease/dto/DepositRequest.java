package com.bankease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DepositRequest {

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be at least 0.01")
    private BigDecimal amount;

    private String description = "Deposit";

    public DepositRequest() {
    }

    public DepositRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description != null ? description : "Deposit";
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
