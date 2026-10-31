package com.bankease.service;

import com.bankease.dto.BeneficiaryRequest;
import com.bankease.dto.BeneficiaryResponse;
import com.bankease.entity.Beneficiary;
import com.bankease.entity.User;
import com.bankease.exception.ResourceNotFoundException;
import com.bankease.repository.BeneficiaryRepository;
import com.bankease.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository, UserRepository userRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BeneficiaryResponse addBeneficiary(String userEmail, BeneficiaryRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        // Check if beneficiary already exists for this user and account
        beneficiaryRepository.findByUserEmailAndAccountNumber(userEmail, request.getAccountNumber().trim())
                .ifPresent(b -> {
                    throw new RuntimeException("Beneficiary with account number " + request.getAccountNumber() + " already exists");
                });

        Beneficiary beneficiary = new Beneficiary(
                user,
                request.getName().trim(),
                request.getAccountNumber().trim(),
                request.getBankName(),
                request.getIfscCode(),
                request.getNickname()
        );

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        return BeneficiaryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(String userEmail) {
        return beneficiaryRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(BeneficiaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBeneficiary(String userEmail, Long id) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found with id: " + id));

        beneficiaryRepository.delete(beneficiary);
    }
}
