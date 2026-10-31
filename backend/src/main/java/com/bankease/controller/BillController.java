package com.bankease.controller;

import com.bankease.dto.BillResponse;
import com.bankease.dto.CreateBillRequest;
import com.bankease.dto.PayBillRequest;
import com.bankease.entity.BillStatus;
import com.bankease.service.BillService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillResponse> createBill(
            Principal principal,
            @Valid @RequestBody CreateBillRequest request
    ) {
        BillResponse response = billService.createBill(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<BillResponse> payBill(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody PayBillRequest request
    ) {
        BillResponse response = billService.payBill(principal.getName(), id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BillResponse>> getBills(
            Principal principal,
            @RequestParam(required = false) BillStatus status
    ) {
        List<BillResponse> bills = billService.getBills(principal.getName(), status);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponse> getBillById(
            Principal principal,
            @PathVariable Long id
    ) {
        BillResponse bill = billService.getBillById(principal.getName(), id);
        return ResponseEntity.ok(bill);
    }
}
