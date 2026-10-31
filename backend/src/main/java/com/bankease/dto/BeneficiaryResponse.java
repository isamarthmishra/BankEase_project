package com.bankease.dto;

import com.bankease.entity.Beneficiary;
import java.time.LocalDateTime;

public class BeneficiaryResponse {

    private Long id;
    private String name;
    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private String nickname;
    private LocalDateTime createdAt;

    public BeneficiaryResponse() {
    }

    public static BeneficiaryResponse fromEntity(Beneficiary beneficiary) {
        BeneficiaryResponse response = new BeneficiaryResponse();
        response.setId(beneficiary.getId());
        response.setName(beneficiary.getName());
        response.setAccountNumber(beneficiary.getAccountNumber());
        response.setBankName(beneficiary.getBankName());
        response.setIfscCode(beneficiary.getIfscCode());
        response.setNickname(beneficiary.getNickname());
        response.setCreatedAt(beneficiary.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
