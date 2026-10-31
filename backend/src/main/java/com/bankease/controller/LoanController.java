package com.bankease.controller;

import com.bankease.dto.ApplyLoanRequest;
import com.bankease.dto.LoanResponse;
import com.bankease.dto.RepayLoanRequest;
import com.bankease.service.LoanService;
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
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanResponse> applyLoan(
            Principal principal,
            @Valid @RequestBody ApplyLoanRequest request
    ) {
        LoanResponse response = loanService.applyAndDisburseLoan(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<LoanResponse> repayLoan(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody RepayLoanRequest request
    ) {
        LoanResponse response = loanService.repayLoan(principal.getName(), id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> getUserLoans(Principal principal) {
        List<LoanResponse> loans = loanService.getUserLoans(principal.getName());
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getLoanById(
            Principal principal,
            @PathVariable Long id
    ) {
        LoanResponse loan = loanService.getLoanById(principal.getName(), id);
        return ResponseEntity.ok(loan);
    }
}
