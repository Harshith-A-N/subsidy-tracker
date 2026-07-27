package com.subsidytracker.security.dto;

import com.subsidytracker.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private String message;
}
