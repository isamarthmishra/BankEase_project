package com.bankease.dto;

import com.bankease.entity.Bill;
import com.bankease.entity.BillCategory;
import com.bankease.entity.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BillResponse {

    private Long id;
    private String billerName;
    private BillCategory category;
    private String consumerNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private BillStatus status;
    private LocalDateTime paidDate;
    private String transactionReference;
    private String paidFromAccount;
    private LocalDateTime createdAt;

    public BillResponse() {
    }

    public static BillResponse fromEntity(Bill bill) {
        BillResponse res = new BillResponse();
        res.setId(bill.getId());
        res.setBillerName(bill.getBillerName());
        res.setCategory(bill.getCategory());
        res.setConsumerNumber(bill.getConsumerNumber());
        res.setAmount(bill.getAmount());
        res.setDueDate(bill.getDueDate());
        res.setStatus(bill.getStatus());
        res.setPaidDate(bill.getPaidDate());
        res.setTransactionReference(bill.getTransactionReference());
        res.setPaidFromAccount(bill.getPaidFromAccount());
        res.setCreatedAt(bill.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillerName() {
        return billerName;
    }

    public void setBillerName(String billerName) {
        this.billerName = billerName;
    }

    public BillCategory getCategory() {
        return category;
    }

    public void setCategory(BillCategory category) {
        this.category = category;
    }

    public String getConsumerNumber() {
        return consumerNumber;
    }

    public void setConsumerNumber(String consumerNumber) {
        this.consumerNumber = consumerNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void setStatus(BillStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDateTime paidDate) {
        this.paidDate = paidDate;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getPaidFromAccount() {
        return paidFromAccount;
    }

    public void setPaidFromAccount(String paidFromAccount) {
        this.paidFromAccount = paidFromAccount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
