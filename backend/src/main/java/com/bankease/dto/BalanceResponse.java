package com.bankease.dto;

import com.bankease.entity.AccountStatus;
import com.bankease.entity.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BalanceResponse {

    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private AccountType accountType;
    private AccountStatus status;
    private LocalDateTime timestamp;

    public BalanceResponse() {
    }

    public BalanceResponse(String accountNumber, BigDecimal balance, String currency, AccountType accountType, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
        this.accountType = accountType;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
