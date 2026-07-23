package com.subsidytracker.beneficiary.service;

import com.subsidytracker.beneficiary.dto.BeneficiaryRequestDto;
import com.subsidytracker.beneficiary.dto.BeneficiaryResponseDto;
import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    @Transactional
    public BeneficiaryResponseDto createBeneficiary(BeneficiaryRequestDto request) {
        beneficiaryRepository.findByNationalIdNumber(request.getNationalIdNumber())
                .ifPresent(b -> {
                    throw new InvalidOperationException(
                            "A beneficiary with this national ID already exists.");
                });

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setFullName(request.getFullName());
        beneficiary.setNationalIdNumber(request.getNationalIdNumber());
        beneficiary.setPhoneNumber(request.getPhoneNumber());
        beneficiary.setAddress(request.getAddress());
        beneficiary.setCategory(request.getCategory());
        beneficiary.setRegion(request.getRegion());
        beneficiary.setRegistrationDate(LocalDate.now());
        beneficiary.setAnnualIncome(request.getAnnualIncome());

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        return toResponseDto(saved);
    }

    public BeneficiaryResponseDto getBeneficiaryById(Long id) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));
        return toResponseDto(beneficiary);
    }

    public List<BeneficiaryResponseDto> getAllBeneficiaries() {
        return beneficiaryRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public BeneficiaryResponseDto updateBeneficiary(Long id, BeneficiaryRequestDto request) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

        beneficiary.setFullName(request.getFullName());
        beneficiary.setPhoneNumber(request.getPhoneNumber());
        beneficiary.setAddress(request.getAddress());
        beneficiary.setCategory(request.getCategory());
        beneficiary.setRegion(request.getRegion());
        beneficiary.setAnnualIncome(request.getAnnualIncome());
        // nationalIdNumber and registrationDate intentionally NOT updatable here

        Beneficiary updated = beneficiaryRepository.save(beneficiary);
        return toResponseDto(updated);
    }

    private BeneficiaryResponseDto toResponseDto(Beneficiary b) {
        BeneficiaryResponseDto dto = new BeneficiaryResponseDto();
        dto.setId(b.getId());
        dto.setFullName(b.getFullName());
        dto.setNationalIdNumber(b.getNationalIdNumber());
        dto.setPhoneNumber(b.getPhoneNumber());
        dto.setAddress(b.getAddress());
        dto.setCategory(b.getCategory());
        dto.setRegistrationDate(b.getRegistrationDate());
        dto.setRegion(b.getRegion());
        dto.setAnnualIncome(b.getAnnualIncome());
        return dto;
    }
}