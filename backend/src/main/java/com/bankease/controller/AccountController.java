package com.bankease.controller;

import com.bankease.dto.AccountResponse;
import com.bankease.dto.BalanceResponse;
import com.bankease.dto.CreateAccountRequest;
import com.bankease.dto.DepositRequest;
import com.bankease.dto.TransferReceiptDto;
import com.bankease.service.AccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            Principal principal,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse response = accountService.createAccount(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Principal principal) {
        List<AccountResponse> accounts = accountService.getAccountsForUser(principal.getName());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountDetails(
            Principal principal,
            @PathVariable String accountNumber
    ) {
        AccountResponse account = accountService.getAccountByNumber(accountNumber, principal.getName());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            Principal principal,
            @PathVariable String accountNumber
    ) {
        BalanceResponse balance = accountService.getBalance(accountNumber, principal.getName());
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<TransferReceiptDto> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request
    ) {
        TransferReceiptDto receipt = accountService.deposit(accountNumber, request);
        return ResponseEntity.ok(receipt);
    }
}
