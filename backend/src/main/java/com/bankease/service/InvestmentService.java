package com.bankease.service;

import com.bankease.dto.CreateInvestmentRequest;
import com.bankease.dto.InvestmentResponse;
import com.bankease.entity.Account;
import com.bankease.entity.Investment;
import com.bankease.entity.InvestmentStatus;
import com.bankease.entity.InvestmentType;
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
import com.bankease.repository.InvestmentRepository;
import com.bankease.repository.TransactionRepository;
import com.bankease.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public InvestmentService(
            InvestmentRepository investmentRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.investmentRepository = investmentRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public InvestmentResponse createInvestment(String userEmail, CreateInvestmentRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Account account = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Funding account not found: " + request.getAccountNumber()));

        if (account.getUser() == null || !account.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to invest from this account");
        }

        if (!account.isActive()) {
            throw new InvalidTransactionException("Account is not active (Status: " + account.getStatus() + ")");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient funds for investment. Available: " + account.getBalance() + ", Requested: " + request.getAmount()
            );
        }

        BigDecimal returnRate = getRateForType(request.getInvestmentType());
        BigDecimal returns = request.getAmount()
                .multiply(returnRate)
                .multiply(BigDecimal.valueOf(request.getTermMonths()))
                .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
        BigDecimal maturityAmount = request.getAmount().add(returns);

        // Deduct investment amount from user account
        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        String investmentNumber = "INV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTermMonths());

        Investment investment = new Investment(
                investmentNumber,
                user,
                account,
                request.getInvestmentType(),
                request.getAmount(),
                returnRate,
                maturityAmount,
                request.getTermMonths(),
                startDate,
                maturityDate
        );
        Investment savedInvestment = investmentRepository.save(investment);

        // Record transaction
        String refNumber = "INV-DEP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction txn = new Transaction(
                refNumber,
                account,
                null,
                request.getAmount(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCESS,
                "Investment in " + request.getInvestmentType() + " #" + investmentNumber,
                null,
                newBalance
        );
        transactionRepository.save(txn);

        return InvestmentResponse.fromEntity(savedInvestment);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public InvestmentResponse withdrawInvestment(String userEmail, Long investmentId) {
        Investment investment = investmentRepository.findByIdAndUserEmail(investmentId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + investmentId));

        if (!InvestmentStatus.ACTIVE.equals(investment.getStatus())) {
            throw new InvalidTransactionException("Investment is not active (Status: " + investment.getStatus() + ")");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(investment.getAccount().getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Linked payout account not found"));

        BigDecimal payoutAmount = investment.getExpectedMaturityAmount();

        // Credit funds back to account
        BigDecimal newBalance = account.getBalance().add(payoutAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Update investment
        investment.setStatus(InvestmentStatus.WITHDRAWN);
        investment.setLiquidatedAt(LocalDateTime.now());
        Investment savedInvestment = investmentRepository.save(investment);

        // Record transaction
        String refNumber = "INV-LIQ-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction txn = new Transaction(
                refNumber,
                null,
                account,
                payoutAmount,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Liquidation of Investment #" + investment.getInvestmentNumber(),
                null,
                newBalance
        );
        transactionRepository.save(txn);

        return InvestmentResponse.fromEntity(savedInvestment);
    }

    @Transactional(readOnly = true)
    public List<InvestmentResponse> getUserInvestments(String userEmail) {
        return investmentRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(InvestmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvestmentResponse getInvestmentById(String userEmail, Long investmentId) {
        Investment investment = investmentRepository.findByIdAndUserEmail(investmentId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + investmentId));
        return InvestmentResponse.fromEntity(investment);
    }

    private BigDecimal getRateForType(InvestmentType type) {
        return switch (type) {
            case FIXED_DEPOSIT -> new BigDecimal("7.25");
            case MUTUAL_FUND -> new BigDecimal("12.00");
            case RECURRING_DEPOSIT -> new BigDecimal("6.80");
            case BONDS -> new BigDecimal("8.10");
        };
    }
}
