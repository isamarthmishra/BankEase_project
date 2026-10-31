package com.bankease.service;

import com.bankease.dto.BillResponse;
import com.bankease.dto.CreateBillRequest;
import com.bankease.dto.PayBillRequest;
import com.bankease.entity.Account;
import com.bankease.entity.Bill;
import com.bankease.entity.BillStatus;
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
import com.bankease.repository.BillRepository;
import com.bankease.repository.TransactionRepository;
import com.bankease.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BillService(
            BillRepository billRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.billRepository = billRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BillResponse createBill(String userEmail, CreateBillRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Bill bill = new Bill(
                user,
                request.getBillerName().trim(),
                request.getCategory(),
                request.getConsumerNumber().trim(),
                request.getAmount(),
                request.getDueDate()
        );

        Bill saved = billRepository.save(bill);
        return BillResponse.fromEntity(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public BillResponse payBill(String userEmail, Long billId, PayBillRequest request) {
        Bill bill = billRepository.findByIdAndUserEmail(billId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + billId));

        if (BillStatus.PAID.equals(bill.getStatus())) {
            throw new InvalidTransactionException("Bill has already been paid on " + bill.getPaidDate());
        }

        Account account = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getUser() == null || !account.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to pay bills using this account");
        }

        if (!account.isActive()) {
            throw new InvalidTransactionException("Account is not active (Status: " + account.getStatus() + ")");
        }

        if (account.getBalance().compareTo(bill.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient funds to pay bill. Available: " + account.getBalance() + ", Bill Amount: " + bill.getAmount()
            );
        }

        // Deduct bill amount from account
        BigDecimal newBalance = account.getBalance().subtract(bill.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Record transaction
        String referenceNumber = "BILL-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = new Transaction(
                referenceNumber,
                account,
                null,
                bill.getAmount(),
                TransactionType.BILL_PAYMENT,
                TransactionStatus.SUCCESS,
                "Bill payment for " + bill.getBillerName() + " (" + bill.getConsumerNumber() + ")",
                null,
                newBalance
        );
        transactionRepository.save(transaction);

        // Update bill details
        bill.setStatus(BillStatus.PAID);
        bill.setPaidDate(LocalDateTime.now());
        bill.setTransactionReference(referenceNumber);
        bill.setPaidFromAccount(account.getAccountNumber());
        Bill savedBill = billRepository.save(bill);

        return BillResponse.fromEntity(savedBill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBills(String userEmail, BillStatus status) {
        List<Bill> bills;
        if (status != null) {
            bills = billRepository.findByUserEmailAndStatus(userEmail, status);
        } else {
            bills = billRepository.findByUserEmail(userEmail);
        }
        return bills.stream().map(BillResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(String userEmail, Long billId) {
        Bill bill = billRepository.findByIdAndUserEmail(billId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + billId));
        return BillResponse.fromEntity(bill);
    }
}
