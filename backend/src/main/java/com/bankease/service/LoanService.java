package com.bankease.service;

import com.bankease.dto.ApplyLoanRequest;
import com.bankease.dto.LoanResponse;
import com.bankease.dto.RepayLoanRequest;
import com.bankease.entity.Account;
import com.bankease.entity.Loan;
import com.bankease.entity.LoanStatus;
import com.bankease.entity.LoanType;
import com.bankease.entity.Transaction;
import com.bankease.entity.TransactionStatus;
import com.bankease.entity.TransactionType;
import com.bankease.entity.User;
import com.bankease.exception.AccountNotFoundException;
import com.bankease.exception.InsufficientBalanceException;
import com.bankease.exception.InvalidTransactionException;
import com.bankease.exception.ResourceNotFoundException;
import com.bankease.exception.UnauthorizedAccessException;
import com.bankease.repository.AccountRepository;
import com.bankease.repository.LoanRepository;
import com.bankease.repository.TransactionRepository;
import com.bankease.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public LoanService(
            LoanRepository loanRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public LoanResponse applyAndDisburseLoan(String userEmail, ApplyLoanRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Account account = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getUser() == null || !account.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to link this account to a loan");
        }

        if (!account.isActive()) {
            throw new InvalidTransactionException("Account is not active (Status: " + account.getStatus() + ")");
        }

        BigDecimal interestRate = getInterestRateForLoanType(request.getLoanType());
        BigDecimal emi = calculateEMI(request.getPrincipalAmount(), interestRate, request.getTermMonths());
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(request.getTermMonths())).setScale(2, RoundingMode.HALF_UP);

        String loanNumber = "LOAN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Loan loan = new Loan(
                loanNumber,
                user,
                account,
                request.getLoanType(),
                request.getPrincipalAmount(),
                interestRate,
                request.getTermMonths(),
                emi,
                totalPayable
        );
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());

        Loan savedLoan = loanRepository.save(loan);

        // Disburse principal amount to the customer's account
        BigDecimal newBalance = account.getBalance().add(request.getPrincipalAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Record loan disbursement transaction
        String refNumber = "DSB-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction txn = new Transaction(
                refNumber,
                null,
                account,
                request.getPrincipalAmount(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Disbursement for " + request.getLoanType() + " Loan #" + loanNumber,
                null,
                newBalance
        );
        transactionRepository.save(txn);

        return LoanResponse.fromEntity(savedLoan);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public LoanResponse repayLoan(String userEmail, Long loanId, RepayLoanRequest request) {
        Loan loan = loanRepository.findByIdAndUserEmail(loanId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (LoanStatus.REPAID.equals(loan.getStatus())) {
            throw new InvalidTransactionException("Loan has already been completely repaid");
        }

        BigDecimal payAmount = request.getAmount();
        if (payAmount.compareTo(loan.getRemainingAmount()) > 0) {
            payAmount = loan.getRemainingAmount();
        }

        Account account = accountRepository.findByAccountNumberForUpdate(loan.getAccount().getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Disbursement account not found"));

        if (account.getBalance().compareTo(payAmount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for loan repayment. Available: " + account.getBalance() + ", Due: " + payAmount
            );
        }

        // Deduct from account
        BigDecimal newBalance = account.getBalance().subtract(payAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Update loan remaining amount
        BigDecimal newRemaining = loan.getRemainingAmount().subtract(payAmount).setScale(2, RoundingMode.HALF_UP);
        loan.setRemainingAmount(newRemaining);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setRemainingAmount(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.REPAID);
        }

        Loan savedLoan = loanRepository.save(loan);

        // Record transaction
        String refNumber = "REP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction txn = new Transaction(
                refNumber,
                account,
                null,
                payAmount,
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCESS,
                "Repayment for Loan #" + loan.getLoanNumber(),
                null,
                newBalance
        );
        transactionRepository.save(txn);

        return LoanResponse.fromEntity(savedLoan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getUserLoans(String userEmail) {
        return loanRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(LoanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(String userEmail, Long loanId) {
        Loan loan = loanRepository.findByIdAndUserEmail(loanId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));
        return LoanResponse.fromEntity(loan);
    }

    private BigDecimal getInterestRateForLoanType(LoanType type) {
        return switch (type) {
            case HOME -> new BigDecimal("8.50");
            case AUTO -> new BigDecimal("9.00");
            case EDUCATION -> new BigDecimal("7.50");
            case PERSONAL -> new BigDecimal("11.50");
        };
    }

    private BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualRate, int months) {
        double p = principal.doubleValue();
        double r = (annualRate.doubleValue() / 12.0) / 100.0;
        int n = months;

        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }
}
