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
public class OfficerRegistrationRequestDto {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private Role requestedRole;
    private String region;
}
