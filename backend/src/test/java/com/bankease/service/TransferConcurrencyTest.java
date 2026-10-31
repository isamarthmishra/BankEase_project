package com.bankease.service;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.AuthResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.RegisterRequest;
import com.bankease.dto.TransferRequest;
import com.bankease.entity.AccountType;
import com.bankease.repository.AccountRepository;
import com.bankease.repository.TransactionRepository;
import com.bankease.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TransferConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private String user1Email;
    private String user2Email;
    private String acc1Number;
    private String acc2Number;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        user1Email = "concurrent1." + ts + "@bankease.com";
        user2Email = "concurrent2." + ts + "@bankease.com";

        authService.register(new RegisterRequest("Conc User 1", user1Email, "password123", "1234567890"));
        authService.register(new RegisterRequest("Conc User 2", user2Email, "password123", "0987654321"));

        AccountResponse r1 = accountService.createAccount(
                user1Email,
                new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("1000.00"), "INR")
        );
        acc1Number = r1.getAccountNumber();

        AccountResponse r2 = accountService.createAccount(
                user2Email,
                new CreateAccountRequest(AccountType.CHECKING, new BigDecimal("1000.00"), "INR")
        );
        acc2Number = r2.getAccountNumber();
    }

    @AfterEach
    void tearDown() {
        try {
            if (acc1Number != null) {
                transactionRepository.deleteAll(transactionRepository.findByAccountNumberOrderByCreatedAtDesc(acc1Number));
                accountRepository.findByAccountNumber(acc1Number).ifPresent(accountRepository::delete);
            }
            if (acc2Number != null) {
                transactionRepository.deleteAll(transactionRepository.findByAccountNumberOrderByCreatedAtDesc(acc2Number));
                accountRepository.findByAccountNumber(acc2Number).ifPresent(accountRepository::delete);
            }
            if (user1Email != null) {
                userRepository.findByEmail(user1Email).ifPresent(userRepository::delete);
            }
            if (user2Email != null) {
                userRepository.findByEmail(user2Email).ifPresent(userRepository::delete);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Should handle 10 concurrent transfers without balance discrepancy or deadlocks")
    void testConcurrentTransfersMaintainBalanceIntegrity() throws Exception {
        int numberOfThreads = 10;
        BigDecimal transferAmount = new BigDecimal("50.00");
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    transferService.transfer(
                            user1Email,
                            new TransferRequest(
                                    acc1Number,
                                    acc2Number,
                                    transferAmount,
                                    "Concurrent transfer " + index,
                                    null
                            )
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Concurrent transfer failed: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // All threads launch concurrently!
        boolean finished = finishLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "All concurrent transfer operations should complete within timeout");
        assertEquals(numberOfThreads, successCount.get(), "All 10 transfers should succeed");

        BigDecimal finalBal1 = accountRepository.findByAccountNumber(acc1Number).get().getBalance();
        BigDecimal finalBal2 = accountRepository.findByAccountNumber(acc2Number).get().getBalance();

        // 1000 - (10 * 50) = 500
        assertEquals(new BigDecimal("500.00"), finalBal1);
        // 1000 + (10 * 50) = 1500
        assertEquals(new BigDecimal("1500.00"), finalBal2);

        // Invariant: Total system balance is strictly preserved (2000.00)
        assertEquals(new BigDecimal("2000.00"), finalBal1.add(finalBal2));
    }
}
