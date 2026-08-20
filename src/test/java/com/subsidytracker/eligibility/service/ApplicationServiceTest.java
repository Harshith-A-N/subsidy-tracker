package com.subsidytracker.eligibility.service;

import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.common.entity.*;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private SchemeRepository schemeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EligibilityService eligibilityService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ApplicationService applicationService;

    private Application application;
    private User user;
    private Beneficiary beneficiary;
    private Scheme scheme;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(20L);
        user.setRole(Role.BENEFICIARY);

        beneficiary = new Beneficiary();
        beneficiary.setId(50L);
        beneficiary.setUser(user);

        scheme = new Scheme();
        scheme.setId(10L);
        scheme.setName("Agri Grant");

        application = new Application();
        application.setId(200L);
        application.setBeneficiary(beneficiary);
        application.setScheme(scheme);
        application.setStatus(ApplicationStatus.DRAFT);
    }

    @Test
    void submitApplication_ShouldSubmitAndLogAudit() {
        ApplicationResponseDto eligibilityResult = new ApplicationResponseDto();
        eligibilityResult.setId(200L);
        eligibilityResult.setStatus(ApplicationStatus.FIELD_VERIFICATION_PENDING);

        when(applicationRepository.findById(200L)).thenReturn(Optional.of(application));
        when(eligibilityService.getMissingMandatoryDocuments(application)).thenReturn(Collections.emptyList());
        when(eligibilityService.calculateEligibilityForApplication(application)).thenReturn(eligibilityResult);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        ApplicationResponseDto response = applicationService.submitApplication(200L, 20L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.FIELD_VERIFICATION_PENDING);
        verify(auditLogService).logEvent(eq("Application"), eq(200L), eq("SUBMITTED"), eq(user), anyString());
    }
}
