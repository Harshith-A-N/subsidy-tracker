package com.subsidytracker.security;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.RequestStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.security.dto.LoginRequestDto;
import com.subsidytracker.security.dto.OfficerRegistrationRequestDto;
import com.subsidytracker.security.dto.OfficerRegistrationResponseDto;
import com.subsidytracker.security.entity.OfficerRegistrationRequest;
import com.subsidytracker.security.repository.OfficerRegistrationRequestRepository;
import com.subsidytracker.security.service.AuthService;
import com.subsidytracker.security.service.OfficerRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OfficerRegistrationServiceTest {

    @Autowired
    private OfficerRegistrationService officerRegistrationService;

    @Autowired
    private OfficerRegistrationRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();

        // Ensure Admin user exists for approval testing
        adminUser = userRepository.findByEmail("admin_test_reg@govgrant.in").orElseGet(() -> {
            User u = new User();
            u.setFullName("Test Admin");
            u.setEmail("admin_test_reg@govgrant.in");
            u.setPassword(passwordEncoder.encode("admin123"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("Valid officer registration creates PENDING request without creating User")
    void testSubmitRequest_Success() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("Rajesh Field Officer");
        dto.setEmail("rajesh_field@govgrant.in");
        dto.setPassword("password123");
        dto.setPhone("9876543210");
        dto.setRequestedRole(Role.FIELD_OFFICER);
        dto.setRegion("Rajasthan");

        OfficerRegistrationResponseDto response = officerRegistrationService.submitRequest(dto);

        assertNotNull(response);
        assertEquals(RequestStatus.PENDING, response.getStatus());
        assertEquals("rajesh_field@govgrant.in", response.getEmail());
        assertEquals(Role.FIELD_OFFICER, response.getRequestedRole());
        assertEquals("Rajasthan", response.getRegion());

        // Verify User was NOT created yet
        assertTrue(userRepository.findByEmail("rajesh_field@govgrant.in").isEmpty());

        // Verify Password is BCrypt hashed in entity
        OfficerRegistrationRequest entity = requestRepository.findById(response.getId()).orElseThrow();
        assertNotEquals("password123", entity.getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", entity.getPasswordHash()));
    }

    @Test
    @DisplayName("Pending officer cannot log in until approved")
    void testPendingOfficerCannotLogin() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("District Officer Pending");
        dto.setEmail("do_pending@govgrant.in");
        dto.setPassword("password123");
        dto.setRequestedRole(Role.DISTRICT_OFFICER);
        dto.setRegion("Gujarat");

        officerRegistrationService.submitRequest(dto);

        LoginRequestDto loginDto = new LoginRequestDto();
        loginDto.setEmail("do_pending@govgrant.in");
        loginDto.setPassword("password123");

        assertThrows(Exception.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Duplicate existing email is rejected for officer registration")
    void testSubmitRequest_DuplicateExistingUser() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("Duplicate User");
        dto.setEmail(adminUser.getEmail());
        dto.setPassword("password123");
        dto.setRequestedRole(Role.FIELD_OFFICER);
        dto.setRegion("Rajasthan");

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> officerRegistrationService.submitRequest(dto)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Duplicate pending email is rejected")
    void testSubmitRequest_DuplicatePending() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("First Request");
        dto.setEmail("pending_dup@govgrant.in");
        dto.setPassword("password123");
        dto.setRequestedRole(Role.FIELD_OFFICER);
        dto.setRegion("Rajasthan");

        officerRegistrationService.submitRequest(dto);

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> officerRegistrationService.submitRequest(dto)
        );
        assertTrue(ex.getMessage().contains("pending officer registration request"));
    }

    @Test
    @DisplayName("ADMIN role cannot be requested")
    void testSubmitRequest_AdminRoleRejected() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("Hacker Admin");
        dto.setEmail("fake_admin@govgrant.in");
        dto.setPassword("password123");
        dto.setRequestedRole(Role.ADMIN);
        dto.setRegion("Rajasthan");

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> officerRegistrationService.submitRequest(dto)
        );
        assertTrue(ex.getMessage().contains("Invalid requested officer role"));
    }

    @Test
    @DisplayName("Admin approval creates User with requested role/region and allows login")
    void testApproveRequest_Success() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("Finance Approver Approved");
        dto.setEmail("fa_approved@govgrant.in");
        dto.setPassword("password123");
        dto.setRequestedRole(Role.FINANCE_APPROVER);
        dto.setRegion("Maharashtra");

        OfficerRegistrationResponseDto submitted = officerRegistrationService.submitRequest(dto);

        OfficerRegistrationResponseDto approved = officerRegistrationService.approveRequest(submitted.getId(), adminUser);

        assertEquals(RequestStatus.APPROVED, approved.getStatus());

        // Verify User is created with correct credentials and role
        User createdUser = userRepository.findByEmail("fa_approved@govgrant.in").orElseThrow();
        assertEquals("Finance Approver Approved", createdUser.getFullName());
        assertEquals(Role.FINANCE_APPROVER, createdUser.getRole());
        assertEquals("Maharashtra", createdUser.getRegion());

        // Verify approved user can now log in via existing JWT login flow
        LoginRequestDto loginDto = new LoginRequestDto();
        loginDto.setEmail("fa_approved@govgrant.in");
        loginDto.setPassword("password123");

        var loginResp = authService.login(loginDto);
        assertNotNull(loginResp.getToken());
        assertEquals(Role.FINANCE_APPROVER, loginResp.getRole());
    }

    @Test
    @DisplayName("Approved request cannot be approved again")
    void testApproveRequest_AlreadyProcessed() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("Double Approve");
        dto.setEmail("double_app@govgrant.in");
        dto.setPassword("password123");
        dto.setRequestedRole(Role.FIELD_OFFICER);
        dto.setRegion("Punjab");

        OfficerRegistrationResponseDto submitted = officerRegistrationService.submitRequest(dto);
        officerRegistrationService.approveRequest(submitted.getId(), adminUser);

        assertThrows(InvalidOperationException.class, () ->
                officerRegistrationService.approveRequest(submitted.getId(), adminUser)
        );
    }

    @Test
    @DisplayName("Admin rejection marks request REJECTED without creating User")
    void testRejectRequest_Success() {
        OfficerRegistrationRequestDto dto = new OfficerRegistrationRequestDto();
        dto.setFullName("Rejected Officer");
        dto.setEmail("rejected_officer@govgrant.in");
        dto.setPassword("password123");
        dto.setRequestedRole(Role.DISTRICT_OFFICER);
        dto.setRegion("Bihar");

        OfficerRegistrationResponseDto submitted = officerRegistrationService.submitRequest(dto);
        OfficerRegistrationResponseDto rejected = officerRegistrationService.rejectRequest(submitted.getId(), "Invalid employee ID", adminUser);

        assertEquals(RequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Invalid employee ID", rejected.getRejectionReason());

        // Verify no User was created
        assertTrue(userRepository.findByEmail("rejected_officer@govgrant.in").isEmpty());

        // Verify rejected request cannot subsequently be approved
        assertThrows(InvalidOperationException.class, () ->
                officerRegistrationService.approveRequest(submitted.getId(), adminUser)
        );
    }
}
