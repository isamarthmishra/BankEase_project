package com.bankease.dto;

import com.bankease.entity.BillCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateBillRequest {

    @NotBlank(message = "Biller name is required")
    private String billerName;

    @NotNull(message = "Bill category is required")
    private BillCategory category;

    @NotBlank(message = "Consumer/Account number with biller is required")
    private String consumerNumber;

    @NotNull(message = "Bill amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    private LocalDate dueDate;

    public CreateBillRequest() {
    }

    public CreateBillRequest(String billerName, BillCategory category, String consumerNumber, BigDecimal amount, LocalDate dueDate) {
        this.billerName = billerName;
        this.category = category;
        this.consumerNumber = consumerNumber;
        this.amount = amount;
        this.dueDate = dueDate;
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
}
