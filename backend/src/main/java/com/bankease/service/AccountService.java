package com.bankease.service;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.BalanceResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.DepositRequest;
import com.bankease.dto.TransferReceiptDto;
import com.bankease.entity.Account;
import com.bankease.entity.AccountStatus;
import com.bankease.entity.Transaction;
import com.bankease.entity.TransactionStatus;
import com.bankease.entity.TransactionType;
import com.bankease.entity.User;
import com.bankease.exception.AccountNotFoundException;
import com.bankease.exception.InvalidTransactionException;
import com.bankease.exception.UnauthorizedAccessException;
import com.bankease.repository.AccountRepository;
import com.bankease.repository.TransactionRepository;
import com.bankease.repository.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse createAccount(String userEmail, CreateAccountRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal initialBalance = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;

        Account account = new Account(
                accountNumber,
                user,
                request.getAccountType(),
                initialBalance,
                request.getCurrency()
        );

        Account savedAccount = accountRepository.save(account);

        // Record initial deposit transaction if positive
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            String referenceNumber = "DEP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Transaction initialTxn = new Transaction(
                    referenceNumber,
                    null,
                    savedAccount,
                    initialBalance,
                    TransactionType.DEPOSIT,
                    TransactionStatus.SUCCESS,
                    "Initial account opening deposit",
                    null,
                    savedAccount.getBalance()
            );
            transactionRepository.save(initialTxn);
        }

        return AccountResponse.fromEntity(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsForUser(String userEmail) {
        return accountRepository.findByUserEmail(userEmail).stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber, String userEmail) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        validateAccountOwnership(account, userEmail);

        return AccountResponse.fromEntity(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber, String userEmail) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        validateAccountOwnership(account, userEmail);

        return new BalanceResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountType(),
                account.getStatus()
        );
    }

    @Transactional
    public TransferReceiptDto deposit(String accountNumber, DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be strictly greater than zero");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (!account.isActive()) {
            throw new InvalidTransactionException("Cannot deposit to account with status " + account.getStatus());
        }

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        String referenceNumber = "DEP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = new Transaction(
                referenceNumber,
                null,
                account,
                request.getAmount(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                request.getDescription() != null ? request.getDescription() : "Account Deposit",
                null,
                newBalance
        );
        Transaction savedTxn = transactionRepository.save(transaction);

        return TransferReceiptDto.fromEntity(savedTxn);
    }

    private void validateAccountOwnership(Account account, String userEmail) {
        if (account.getUser() == null || !account.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to access account " + account.getAccountNumber());
        }
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 100000000000L + (long) (secureRandom.nextDouble() * 900000000000L);
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
