package com.subsidytracker.security.service;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.RequestStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.security.dto.OfficerRegistrationRequestDto;
import com.subsidytracker.security.dto.OfficerRegistrationResponseDto;
import com.subsidytracker.security.entity.OfficerRegistrationRequest;
import com.subsidytracker.security.repository.OfficerRegistrationRequestRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfficerRegistrationService {

    private final OfficerRegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public OfficerRegistrationService(OfficerRegistrationRequestRepository requestRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuditLogService auditLogService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OfficerRegistrationResponseDto submitRequest(OfficerRegistrationRequestDto dto) {
        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            throw new InvalidOperationException("Full name is required.");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank() || !dto.getEmail().contains("@")) {
            throw new InvalidOperationException("Valid email address is required.");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new InvalidOperationException("Password must be at least 6 characters.");
        }
        if (dto.getRequestedRole() == null
                || dto.getRequestedRole() == Role.ADMIN
                || dto.getRequestedRole() == Role.BENEFICIARY) {
            throw new InvalidOperationException("Invalid requested officer role. Must be FIELD_OFFICER, DISTRICT_OFFICER, or FINANCE_APPROVER.");
        }

        if (dto.getRequestedRole() == Role.FIELD_OFFICER || dto.getRequestedRole() == Role.DISTRICT_OFFICER) {
            if (dto.getRegion() == null || dto.getRegion().isBlank()) {
                throw new InvalidOperationException("Region / State is required for " + dto.getRequestedRole().name() + ".");
            }
        }

        String email = dto.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new InvalidOperationException("An account with this email already exists.");
        }

        if (requestRepository.existsByEmailAndStatus(email, RequestStatus.PENDING)) {
            throw new InvalidOperationException("A pending officer registration request for this email already exists.");
        }

        OfficerRegistrationRequest request = new OfficerRegistrationRequest();
        request.setFullName(dto.getFullName().trim());
        request.setEmail(email);
        request.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        request.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        request.setRequestedRole(dto.getRequestedRole());
        request.setRegion(dto.getRegion() != null ? dto.getRegion().trim() : null);
        request.setStatus(RequestStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());

        OfficerRegistrationRequest saved = requestRepository.save(request);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OfficerRegistrationResponseDto> getPendingRequests() {
        return requestRepository.findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfficerRegistrationResponseDto> getAllRequests() {
        return requestRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OfficerRegistrationResponseDto approveRequest(Long requestId, User adminUser) {
        OfficerRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("OfficerRegistrationRequest", requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Request has already been processed.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new InvalidOperationException("User account for email '" + request.getEmail() + "' already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPasswordHash()); // already BCrypt hashed
        user.setRole(request.getRequestedRole());
        user.setRegion(request.getRegion());

        User savedUser = userRepository.save(user);

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminUser);

        OfficerRegistrationRequest savedRequest = requestRepository.save(request);

        auditLogService.logEvent(
                "OfficerRegistrationRequest",
                savedRequest.getId(),
                "OFFICER_REGISTRATION_APPROVED",
                adminUser,
                "Approved officer registration for email: " + savedRequest.getEmail()
                        + " with role: " + savedRequest.getRequestedRole()
                        + " (Created User ID: " + savedUser.getId() + ")"
        );

        return mapToDto(savedRequest);
    }

    @Transactional
    public OfficerRegistrationResponseDto rejectRequest(Long requestId, String rejectionReason, User adminUser) {
        OfficerRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("OfficerRegistrationRequest", requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Request has already been processed.");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(rejectionReason != null && !rejectionReason.isBlank() ? rejectionReason.trim() : "Rejected by Administrator");
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminUser);

        OfficerRegistrationRequest savedRequest = requestRepository.save(request);

        auditLogService.logEvent(
                "OfficerRegistrationRequest",
                savedRequest.getId(),
                "OFFICER_REGISTRATION_REJECTED",
                adminUser,
                "Rejected officer registration for email: " + savedRequest.getEmail()
                        + ". Reason: " + savedRequest.getRejectionReason()
        );

        return mapToDto(savedRequest);
    }

    private OfficerRegistrationResponseDto mapToDto(OfficerRegistrationRequest req) {
        OfficerRegistrationResponseDto dto = new OfficerRegistrationResponseDto();
        dto.setId(req.getId());
        dto.setFullName(req.getFullName());
        dto.setEmail(req.getEmail());
        dto.setPhone(req.getPhone());
        dto.setRequestedRole(req.getRequestedRole());
        dto.setRegion(req.getRegion());
        dto.setStatus(req.getStatus());
        dto.setSubmittedAt(req.getSubmittedAt());
        dto.setReviewedAt(req.getReviewedAt());
        if (req.getReviewedBy() != null) {
            dto.setReviewedByEmail(req.getReviewedBy().getEmail());
        }
        dto.setRejectionReason(req.getRejectionReason());
        return dto;
    }
}
