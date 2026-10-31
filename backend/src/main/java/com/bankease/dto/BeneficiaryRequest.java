package com.bankease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BeneficiaryRequest {

    @NotBlank(message = "Beneficiary name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Account number is required")
    @Size(min = 5, max = 30, message = "Account number must be between 5 and 30 characters")
    private String accountNumber;

    private String bankName = "BankEase";

    private String ifscCode = "BANKEASE001";

    @Size(max = 50, message = "Nickname cannot exceed 50 characters")
    private String nickname;

    public BeneficiaryRequest() {
    }

    public BeneficiaryRequest(String name, String accountNumber, String bankName, String ifscCode, String nickname) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.bankName = bankName != null ? bankName : "BankEase";
        this.ifscCode = ifscCode != null ? ifscCode : "BANKEASE001";
        this.nickname = nickname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
