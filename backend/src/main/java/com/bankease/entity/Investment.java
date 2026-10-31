package com.bankease.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "investments",
        indexes = {
                @Index(name = "idx_investment_number", columnList = "investment_number", unique = true)
        }
)
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investment_number", nullable = false, unique = true, length = 64)
    private String investmentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_type", nullable = false, length = 30)
    private InvestmentType investmentType;

    @Column(name = "invested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "return_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal returnRate;

    @Column(name = "expected_maturity_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal expectedMaturityAmount;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvestmentStatus status = InvestmentStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "liquidated_at")
    private LocalDateTime liquidatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Investment() {
    }

    public Investment(
            String investmentNumber,
            User user,
            Account account,
            InvestmentType investmentType,
            BigDecimal investedAmount,
            BigDecimal returnRate,
            BigDecimal expectedMaturityAmount,
            Integer termMonths,
            LocalDate startDate,
            LocalDate maturityDate
    ) {
        this.investmentNumber = investmentNumber;
        this.user = user;
        this.account = account;
        this.investmentType = investmentType;
        this.investedAmount = investedAmount;
        this.returnRate = returnRate;
        this.expectedMaturityAmount = expectedMaturityAmount;
        this.termMonths = termMonths;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.status = InvestmentStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = InvestmentStatus.ACTIVE;
        }
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
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
