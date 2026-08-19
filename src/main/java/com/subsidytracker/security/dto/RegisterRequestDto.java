package com.subsidytracker.security.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {
    private String fullName;
    private String email;
    private String password;
}
