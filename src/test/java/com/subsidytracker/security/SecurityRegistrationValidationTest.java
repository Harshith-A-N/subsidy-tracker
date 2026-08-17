package com.subsidytracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subsidytracker.security.dto.RegisterRequestDto;
import com.subsidytracker.security.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityRegistrationValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    public void testRegister_BlankFullName_ShouldReturnBadRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("", "valid@email.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_BlankEmail_ShouldReturnBadRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("John Doe", "", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_MalformedEmail_ShouldReturnBadRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("John Doe", "invalid-email", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_BlankPassword_ShouldReturnBadRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("John Doe", "valid@email.com", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_ShortPassword_ShouldReturnBadRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("John Doe", "valid@email.com", "1234567");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_ValidPayload_ShouldNotBeRejectedByValidation() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("John Doe", "valid@email.com", "password123");

        // Mock authService behavior so it doesn't fail downstream
        when(authService.registerBeneficiary(any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(not(400)));
    }
}
