package com.bankease.service;

import com.bankease.dto.TransferReceiptDto;
import com.bankease.entity.Account;
import com.bankease.entity.Transaction;
import com.bankease.exception.AccountNotFoundException;
import com.bankease.exception.ResourceNotFoundException;
import com.bankease.exception.UnauthorizedAccessException;
import com.bankease.repository.AccountRepository;
import com.bankease.repository.TransactionRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<TransferReceiptDto> getTransactionsForAccount(String userEmail, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getUser() == null || !account.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to view transactions for account " + accountNumber);
        }

        return transactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).stream()
                .map(TransferReceiptDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransferReceiptDto getTransactionReceipt(String userEmail, String referenceNumber) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction receipt not found: " + referenceNumber));

        boolean isAuthorized = false;
        if (transaction.getSourceAccount() != null && transaction.getSourceAccount().getUser() != null) {
            if (transaction.getSourceAccount().getUser().getEmail().equalsIgnoreCase(userEmail)) {
                isAuthorized = true;
            }
        }
        if (transaction.getTargetAccount() != null && transaction.getTargetAccount().getUser() != null) {
            if (transaction.getTargetAccount().getUser().getEmail().equalsIgnoreCase(userEmail)) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            throw new UnauthorizedAccessException("You are not authorized to view this transaction receipt");
        }

        return TransferReceiptDto.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransferReceiptDto> getAllTransactionsForUser(String userEmail) {
        return transactionRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(TransferReceiptDto::fromEntity)
                .collect(Collectors.toList());
    }
}
