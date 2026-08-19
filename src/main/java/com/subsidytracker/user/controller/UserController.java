package com.subsidytracker.user.controller;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.user.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Small, targeted addition (frontend integration Step 0):
 * - GET /me lets any logged-in user (officer, admin, beneficiary) resolve
 *   their own id/role/region without decoding the JWT client-side. Officers
 *   need this so the portal can label/filter "applications in my region"
 *   before they open one.
 * - GET (list) lets the Admin dashboard populate an "All Users" screen.
 *   Restricted to ADMIN in SecurityConfig.
 *
 * No password field is ever included — see UserResponseDto.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));
        return ResponseEntity.ok(toDto(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAll() {
        List<UserResponseDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    private UserResponseDto toDto(User u) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setFullName(u.getFullName());
        dto.setRole(u.getRole());
        dto.setRegion(u.getRegion());
        return dto;
    }
}
