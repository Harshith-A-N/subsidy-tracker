package com.subsidytracker.common.service;

import com.subsidytracker.common.entity.AuditLog;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.repository.AuditLogRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(5L);
        user.setEmail("auditor@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logEvent_withExplicitUser_savesAuditLogWithUser() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.logEvent("Application", 100L, "STATUS_CHANGE", user, "Status changed to ELIGIBLE");

        assertThat(result).isNotNull();
        assertThat(result.getEntityName()).isEqualTo("Application");
        assertThat(result.getEntityId()).isEqualTo(100L);
        assertThat(result.getAction()).isEqualTo("STATUS_CHANGE");
        assertThat(result.getActor()).isEqualTo(user);
        assertThat(result.getDetails()).contains("Status changed to ELIGIBLE");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logEvent_withSecurityContextFallback_resolvesAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor@test.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(userRepository.findByEmail("auditor@test.com")).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.logEvent("Document", 200L, "DOCUMENT_VERIFY", (User) null, "Verified document");

        assertThat(result.getActor()).isEqualTo(user);
        assertThat(result.getEntityName()).isEqualTo("Document");
        verify(userRepository).findByEmail("auditor@test.com");
    }

    @Test
    void logEvent_withUserId_fetchesUserAndSavesLog() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.logEvent("Application", 100L, "APPLICATION_SUBMITTED", 5L, "Submitted application");

        assertThat(result.getActor()).isEqualTo(user);
        verify(userRepository).findById(5L);
    }

    @Test
    void logSystemEvent_savesLogWithNullActor() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.logSystemEvent("System", 1L, "DAILY_OVERDUE_CHECK", "System check executed");

        assertThat(result.getActor()).isNull();
        assertThat(result.getAction()).isEqualTo("DAILY_OVERDUE_CHECK");
    }

    @Test
    void getAuditLogsForEntity_delegatesToRepository() {
        AuditLog log1 = new AuditLog();
        log1.setEntityName("Application");
        log1.setEntityId(10L);

        when(auditLogRepository.findByEntityNameAndEntityId("Application", 10L)).thenReturn(List.of(log1));

        List<AuditLog> logs = auditLogService.getAuditLogsForEntity("Application", 10L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEntityId()).isEqualTo(10L);
    }
}
