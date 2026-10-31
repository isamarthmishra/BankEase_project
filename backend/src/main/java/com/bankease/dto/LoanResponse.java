package com.bankease.dto;

import com.bankease.entity.Loan;
import com.bankease.entity.LoanStatus;
import com.bankease.entity.LoanType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanResponse {

    private Long id;
    private String loanNumber;
    private String accountNumber;
    private LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private BigDecimal monthlyInstallment;
    private BigDecimal remainingAmount;
    private BigDecimal totalPayable;
    private LoanStatus status;
    private LocalDateTime disbursedAt;
    private LocalDateTime createdAt;

    public LoanResponse() {
    }

    public static LoanResponse fromEntity(Loan loan) {
        LoanResponse res = new LoanResponse();
        res.setId(loan.getId());
        res.setLoanNumber(loan.getLoanNumber());
        if (loan.getAccount() != null) {
            res.setAccountNumber(loan.getAccount().getAccountNumber());
        }
        res.setLoanType(loan.getLoanType());
        res.setPrincipalAmount(loan.getPrincipalAmount());
        res.setInterestRate(loan.getInterestRate());
        res.setTermMonths(loan.getTermMonths());
        res.setMonthlyInstallment(loan.getMonthlyInstallment());
        res.setRemainingAmount(loan.getRemainingAmount());
        res.setTotalPayable(loan.getTotalPayable());
        res.setStatus(loan.getStatus());
        res.setDisbursedAt(loan.getDisbursedAt());
        res.setCreatedAt(loan.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoanNumber() {
        return loanNumber;
    }

    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
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

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public BigDecimal getMonthlyInstallment() {
        return monthlyInstallment;
    }

    public BigDecimal getMonthlyEmi() {
        return monthlyInstallment;
    }

    public void setMonthlyInstallment(BigDecimal monthlyInstallment) {
        this.monthlyInstallment = monthlyInstallment;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getTotalPayable() {
        return totalPayable;
    }

    public void setTotalPayable(BigDecimal totalPayable) {
        this.totalPayable = totalPayable;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
