package com.bankease.dto;

import com.bankease.entity.Investment;
import com.bankease.entity.InvestmentStatus;
import com.bankease.entity.InvestmentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvestmentResponse {

    private Long id;
    private String investmentNumber;
    private String accountNumber;
    private InvestmentType investmentType;
    private BigDecimal investedAmount;
    private BigDecimal returnRate;
    private BigDecimal expectedMaturityAmount;
    private Integer termMonths;
    private InvestmentStatus status;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private LocalDateTime liquidatedAt;
    private LocalDateTime createdAt;

    public InvestmentResponse() {
    }

    public static InvestmentResponse fromEntity(Investment investment) {
        InvestmentResponse res = new InvestmentResponse();
        res.setId(investment.getId());
        res.setInvestmentNumber(investment.getInvestmentNumber());
        if (investment.getAccount() != null) {
            res.setAccountNumber(investment.getAccount().getAccountNumber());
        }
        res.setInvestmentType(investment.getInvestmentType());
        res.setInvestedAmount(investment.getInvestedAmount());
        res.setReturnRate(investment.getReturnRate());
        res.setExpectedMaturityAmount(investment.getExpectedMaturityAmount());
        res.setTermMonths(investment.getTermMonths());
        res.setStatus(investment.getStatus());
        res.setStartDate(investment.getStartDate());
        res.setMaturityDate(investment.getMaturityDate());
        res.setLiquidatedAt(investment.getLiquidatedAt());
        res.setCreatedAt(investment.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvestmentNumber() {
        return investmentNumber;
    }

    public void setInvestmentNumber(String investmentNumber) {
        this.investmentNumber = investmentNumber;
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

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount;
    }

    public BigDecimal getReturnRate() {
        return returnRate;
    }

    public void setReturnRate(BigDecimal returnRate) {
        this.returnRate = returnRate;
    }

    public BigDecimal getExpectedMaturityAmount() {
        return expectedMaturityAmount;
    }

    public void setExpectedMaturityAmount(BigDecimal expectedMaturityAmount) {
        this.expectedMaturityAmount = expectedMaturityAmount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(InvestmentStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public LocalDateTime getLiquidatedAt() {
        return liquidatedAt;
    }

    public void setLiquidatedAt(LocalDateTime liquidatedAt) {
        this.liquidatedAt = liquidatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
