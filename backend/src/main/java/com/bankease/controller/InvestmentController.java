package com.bankease.controller;

import com.bankease.dto.CreateInvestmentRequest;
import com.bankease.dto.InvestmentResponse;
import com.bankease.service.InvestmentService;
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
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @PostMapping
    public ResponseEntity<InvestmentResponse> createInvestment(
            Principal principal,
            @Valid @RequestBody CreateInvestmentRequest request
    ) {
        InvestmentResponse response = investmentService.createInvestment(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<InvestmentResponse> withdrawInvestment(
            Principal principal,
            @PathVariable Long id
    ) {
        InvestmentResponse response = investmentService.withdrawInvestment(principal.getName(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InvestmentResponse>> getUserInvestments(Principal principal) {
        List<InvestmentResponse> investments = investmentService.getUserInvestments(principal.getName());
        return ResponseEntity.ok(investments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestmentResponse> getInvestmentById(
            Principal principal,
            @PathVariable Long id
    ) {
        InvestmentResponse investment = investmentService.getInvestmentById(principal.getName(), id);
        return ResponseEntity.ok(investment);
    }
}
