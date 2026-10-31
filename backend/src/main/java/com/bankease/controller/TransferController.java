package com.bankease.controller;

import com.bankease.dto.TransferReceiptDto;
import com.bankease.dto.TransferRequest;
import com.bankease.service.TransferService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferReceiptDto> executeTransfer(
            Principal principal,
            @Valid @RequestBody TransferRequest request
    ) {
        TransferReceiptDto receipt = transferService.transfer(principal.getName(), request);
        return ResponseEntity.ok(receipt);
    }
}
