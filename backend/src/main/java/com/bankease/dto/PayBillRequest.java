package com.bankease.dto;

import jakarta.validation.constraints.NotBlank;

public class PayBillRequest {

    @NotBlank(message = "Account number to pay from is required")
    private String accountNumber;

    public PayBillRequest() {
    }

    public PayBillRequest(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
