package com.bankease.dto;

import com.bankease.entity.InvestmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateInvestmentRequest {

    @NotBlank(message = "Funding account number is required")
    private String accountNumber;

    @NotNull(message = "Investment type is required (FIXED_DEPOSIT, MUTUAL_FUND, RECURRING_DEPOSIT, BONDS)")
    private InvestmentType investmentType;

    @NotNull(message = "Invested amount is required")
    @DecimalMin(value = "500.00", message = "Invested amount must be at least 500")
    private BigDecimal amount;

    @NotNull(message = "Term in months is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    private Integer termMonths;

    public CreateInvestmentRequest() {
    }

    public CreateInvestmentRequest(String accountNumber, InvestmentType investmentType, BigDecimal amount, Integer termMonths) {
        this.accountNumber = accountNumber;
        this.investmentType = investmentType;
        this.amount = amount;
        this.termMonths = termMonths;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public InvestmentType getInvestmentType() {
        return investmentType;
    }

    public void setInvestmentType(InvestmentType investmentType) {
        this.investmentType = investmentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }
}
