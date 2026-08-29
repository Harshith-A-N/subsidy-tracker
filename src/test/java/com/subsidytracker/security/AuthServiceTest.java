package com.subsidytracker.security;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.security.dto.AuthResponseDto;
import com.subsidytracker.security.dto.LoginRequestDto;
import com.subsidytracker.security.dto.RegisterRequestDto;
import com.subsidytracker.security.service.AuthService;
import com.subsidytracker.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerBeneficiary_Successful_ShouldCreateBeneficiaryAndReturnJwtWithRole() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFullName("John Beneficiary");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken("john@example.com", "BENEFICIARY")).thenReturn("mocked.jwt.token");

        AuthResponseDto response = authService.registerBeneficiary(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo(Role.BENEFICIARY);
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        verify(jwtService).generateToken("john@example.com", "BENEFICIARY");
    }

    @Test
    void login_Successful_ShouldAuthenticateAndReturnJwtWithRole() {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("secret123");

        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setRole(Role.BENEFICIARY);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("user@example.com", "BENEFICIARY")).thenReturn("mocked.jwt.token");

        AuthResponseDto response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getRole()).isEqualTo(Role.BENEFICIARY);
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        verify(jwtService).generateToken("user@example.com", "BENEFICIARY");
    }

    @Test
    void login_InvalidPassword_ShouldThrowBadCredentialsException() {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void login_UnknownUser_ShouldThrowBadCredentialsException() {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("unknown@example.com");
        loginRequest.setPassword("anyPassword");

        doThrow(new BadCredentialsException("User not found"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void login_RoleMismatch_ShouldThrowInvalidOperationException() {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("secret123");
        loginRequest.setRole(Role.ADMIN); // Requested ADMIN, but user is BENEFICIARY

        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setRole(Role.BENEFICIARY);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(com.subsidytracker.common.exception.InvalidOperationException.class)
                .hasMessageContaining("role mismatch");

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void login_MatchingRole_ShouldSucceed() {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("secret123");
        loginRequest.setRole(Role.BENEFICIARY);

        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setRole(Role.BENEFICIARY);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("user@example.com", "BENEFICIARY")).thenReturn("mocked.jwt.token");

        AuthResponseDto response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.BENEFICIARY);
    }
}
