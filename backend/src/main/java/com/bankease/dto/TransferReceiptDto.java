package com.bankease.dto;

import com.bankease.entity.Transaction;
import com.bankease.entity.TransactionStatus;
import com.bankease.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferReceiptDto {

    private String referenceNumber;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
    private BigDecimal balanceAfter;
    private String failureReason;
    private LocalDateTime timestamp;

    public TransferReceiptDto() {
    }

    public static TransferReceiptDto fromEntity(Transaction transaction) {
        TransferReceiptDto receipt = new TransferReceiptDto();
        receipt.setReferenceNumber(transaction.getReferenceNumber());
        receipt.setTransactionType(transaction.getTransactionType());
        receipt.setStatus(transaction.getStatus());
        if (transaction.getSourceAccount() != null) {
            receipt.setSourceAccountNumber(transaction.getSourceAccount().getAccountNumber());
            receipt.setCurrency(transaction.getSourceAccount().getCurrency());
        }
        if (transaction.getTargetAccount() != null) {
            receipt.setTargetAccountNumber(transaction.getTargetAccount().getAccountNumber());
            if (receipt.getCurrency() == null) {
                receipt.setCurrency(transaction.getTargetAccount().getCurrency());
            }
        }
        receipt.setAmount(transaction.getAmount());
        receipt.setDescription(transaction.getDescription());
        receipt.setBalanceAfter(transaction.getBalanceAfter());
        receipt.setFailureReason(transaction.getFailureReason());
        receipt.setTimestamp(transaction.getCreatedAt());
        return receipt;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
