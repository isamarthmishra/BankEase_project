package com.bankease.controller;

import com.bankease.dto.TransferReceiptDto;
import com.bankease.service.TransactionService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransferReceiptDto>> getAllMyTransactions(Principal principal) {
        List<TransferReceiptDto> transactions = transactionService.getAllTransactionsForUser(principal.getName());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransferReceiptDto>> getAccountTransactions(
            Principal principal,
            @PathVariable String accountNumber
    ) {
        List<TransferReceiptDto> transactions = transactionService.getTransactionsForAccount(principal.getName(), accountNumber);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/receipt/{referenceNumber}")
    public ResponseEntity<TransferReceiptDto> getReceipt(
            Principal principal,
            @PathVariable String referenceNumber
    ) {
        TransferReceiptDto receipt = transactionService.getTransactionReceipt(principal.getName(), referenceNumber);
        return ResponseEntity.ok(receipt);
    }
}
