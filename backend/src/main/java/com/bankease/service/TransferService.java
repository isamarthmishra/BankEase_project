package com.bankease.service;

import com.bankease.dto.TransferReceiptDto;
import com.bankease.dto.TransferRequest;
import com.bankease.entity.Account;
import com.bankease.entity.Transaction;
import com.bankease.entity.TransactionStatus;
import com.bankease.entity.TransactionType;
import com.bankease.exception.AccountNotFoundException;
import com.bankease.exception.InsufficientBalanceException;
import com.bankease.exception.InvalidTransactionException;
import com.bankease.exception.UnauthorizedAccessException;
import com.bankease.repository.AccountRepository;
import com.bankease.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransferReceiptDto transfer(String userEmail, TransferRequest request) {
        // 1. Basic validation
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be strictly greater than zero");
        }

        String sourceNumber = request.getSourceAccountNumber().trim();
        String targetNumber = request.getTargetAccountNumber().trim();

        if (sourceNumber.equalsIgnoreCase(targetNumber)) {
            throw new InvalidTransactionException("Source and target accounts cannot be the same");
        }

        // 2. Idempotency check: prevent duplicate transactions if idempotency key is supplied
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Transaction> existingTxn = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existingTxn.isPresent()) {
                return TransferReceiptDto.fromEntity(existingTxn.get());
            }
        }

        // 3. Acquire pessimistic locks in deterministic sorted order to prevent deadlocks
        String firstAccNumber = sourceNumber.compareTo(targetNumber) < 0 ? sourceNumber : targetNumber;
        String secondAccNumber = sourceNumber.compareTo(targetNumber) < 0 ? targetNumber : sourceNumber;

        Account firstAccount = accountRepository.findByAccountNumberForUpdate(firstAccNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstAccNumber));
        Account secondAccount = accountRepository.findByAccountNumberForUpdate(secondAccNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondAccNumber));

        Account sourceAccount = sourceNumber.equals(firstAccount.getAccountNumber()) ? firstAccount : secondAccount;
        Account targetAccount = targetNumber.equals(firstAccount.getAccountNumber()) ? firstAccount : secondAccount;

        // 4. Validate ownership of source account
        if (sourceAccount.getUser() == null || !sourceAccount.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to transfer funds from account " + sourceNumber);
        }

        // 5. Validate status of both accounts
        if (!sourceAccount.isActive()) {
            throw new InvalidTransactionException("Source account " + sourceNumber + " is not active (Status: " + sourceAccount.getStatus() + ")");
        }
        if (!targetAccount.isActive()) {
            throw new InvalidTransactionException("Target account " + targetNumber + " is not active (Status: " + targetAccount.getStatus() + ")");
        }

        // 6. Validate currency compatibility
        if (!sourceAccount.getCurrency().equalsIgnoreCase(targetAccount.getCurrency())) {
            throw new InvalidTransactionException(
                    "Currency mismatch between source (" + sourceAccount.getCurrency() + ") and target (" + targetAccount.getCurrency() + ")"
            );
        }

        // 7. Validate sufficient balance
        BigDecimal transferAmount = request.getAmount();
        if (sourceAccount.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient funds in account " + sourceNumber + ". Current balance: " + sourceAccount.getBalance() + ", Requested: " + transferAmount
            );
        }

        // 8. Execute balance updates
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(transferAmount);
        BigDecimal newTargetBalance = targetAccount.getBalance().add(transferAmount);

        sourceAccount.setBalance(newSourceBalance);
        targetAccount.setBalance(newTargetBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 9. Generate transaction receipt record
        String referenceNumber = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : "Transfer to " + targetNumber;

        Transaction transaction = new Transaction(
                referenceNumber,
                sourceAccount,
                targetAccount,
                transferAmount,
                TransactionType.TRANSFER,
                TransactionStatus.SUCCESS,
                description,
                request.getIdempotencyKey() != null ? request.getIdempotencyKey().trim() : null,
                newSourceBalance
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransferReceiptDto.fromEntity(savedTransaction);
    }
}
