package com.bankease.controller;

import com.bankease.dto.BeneficiaryRequest;
import com.bankease.dto.BeneficiaryResponse;
import com.bankease.service.BeneficiaryService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(
            Principal principal,
            @Valid @RequestBody BeneficiaryRequest request
    ) {
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiaries(Principal principal) {
        List<BeneficiaryResponse> list = beneficiaryService.getBeneficiaries(principal.getName());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBeneficiary(
            Principal principal,
            @PathVariable Long id
    ) {
        beneficiaryService.deleteBeneficiary(principal.getName(), id);
        return ResponseEntity.ok("Beneficiary deleted successfully");
    }
}
