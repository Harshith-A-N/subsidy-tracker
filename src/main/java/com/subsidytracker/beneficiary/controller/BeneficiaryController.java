package com.subsidytracker.beneficiary.controller;

import com.subsidytracker.beneficiary.dto.BeneficiaryRequestDto;
import com.subsidytracker.beneficiary.dto.BeneficiaryResponseDto;
import com.subsidytracker.beneficiary.service.BeneficiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponseDto> create(@RequestBody BeneficiaryRequestDto request) {
        BeneficiaryResponseDto created = beneficiaryService.createBeneficiary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(id));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponseDto>> getAll() {
        return ResponseEntity.ok(beneficiaryService.getAllBeneficiaries());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BeneficiaryResponseDto> update(@PathVariable Long id,
                                                         @RequestBody BeneficiaryRequestDto request) {
        return ResponseEntity.ok(beneficiaryService.updateBeneficiary(id, request));
    }
}