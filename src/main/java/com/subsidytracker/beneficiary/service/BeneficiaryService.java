package com.subsidytracker.beneficiary.service;

import com.subsidytracker.beneficiary.dto.BeneficiaryRequestDto;
import com.subsidytracker.beneficiary.dto.BeneficiaryResponseDto;
import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                              UserRepository userRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a beneficiary profile linked to the authenticated user.
     * Enforces one profile per account and prevents duplicate national IDs.
     */
    @Transactional
    public BeneficiaryResponseDto createBeneficiary(BeneficiaryRequestDto request, long currentUserId) {
        // Ensure the user exists and is a beneficiary
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (user.getRole() != Role.BENEFICIARY) {
            throw new InvalidOperationException("Only users with BENEFICIARY role can create a beneficiary profile.");
        }

        // One profile per account
        beneficiaryRepository.findByUserId(currentUserId)
                .ifPresent(b -> {
                    throw new InvalidOperationException(
                            "A beneficiary profile already exists for this account.");
                });

        // Duplicate national ID check (preserved from original)
        beneficiaryRepository.findByNationalIdNumber(request.getNationalIdNumber())
                .ifPresent(b -> {
                    throw new InvalidOperationException(
                            "A beneficiary with this national ID already exists.");
                });

        if (request.getAnnualIncome() != null && request.getAnnualIncome().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Annual income cannot be negative.");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setFullName(request.getFullName());
        beneficiary.setNationalIdNumber(request.getNationalIdNumber());
        beneficiary.setPhoneNumber(request.getPhoneNumber());
        beneficiary.setAddress(request.getAddress());
        beneficiary.setCategory(request.getCategory());
        beneficiary.setRegion(request.getRegion());
        beneficiary.setRegistrationDate(LocalDate.now());
        beneficiary.setAnnualIncome(request.getAnnualIncome());
        beneficiary.setUser(user);

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        return toResponseDto(saved);
    }

    /**
     * Returns the beneficiary profile for the currently logged-in user.
     */
    public BeneficiaryResponseDto getMyProfile(long currentUserId) {
        Beneficiary beneficiary = beneficiaryRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No beneficiary profile found for current user."));
        return toResponseDto(beneficiary);
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

    public org.springframework.data.domain.Page<BeneficiaryResponseDto> getAllBeneficiaries(org.springframework.data.domain.Pageable pageable) {
        return beneficiaryRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    /**
     * Updates a beneficiary profile.
     * The caller must be the owner of this profile or an ADMIN.
     */
    @Transactional
    public BeneficiaryResponseDto updateBeneficiary(Long id, BeneficiaryRequestDto request, long currentUserId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

        // Ownership check: only the owner or an admin may update
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        boolean isOwner = beneficiary.getUser() != null
                && beneficiary.getUser().getId() == currentUserId;
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new InvalidOperationException("You are not authorized to update this beneficiary profile.");
        }

        if (request.getAnnualIncome() != null && request.getAnnualIncome().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Annual income cannot be negative.");
        }

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
        if (b.getUser() != null) {
            dto.setUserId(b.getUser().getId());
            dto.setEmail(b.getUser().getEmail());
        }
        return dto;
    }
}