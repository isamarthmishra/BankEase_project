package com.bankease.dto;

import com.bankease.entity.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ApplyLoanRequest {

    @NotBlank(message = "Disbursement and repayment account number is required")
    private String accountNumber;

    @NotNull(message = "Loan type is required (PERSONAL, HOME, AUTO, EDUCATION)")
    private LoanType loanType;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Principal amount must be at least 1,000")
    private BigDecimal principalAmount;

    @NotNull(message = "Term in months is required")
    @Min(value = 3, message = "Loan term must be at least 3 months")
    private Integer termMonths;

    public ApplyLoanRequest() {
    }

    public ApplyLoanRequest(String accountNumber, LoanType loanType, BigDecimal principalAmount, Integer termMonths) {
        this.accountNumber = accountNumber;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.termMonths = termMonths;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }
}
