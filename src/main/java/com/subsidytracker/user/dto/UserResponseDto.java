package com.subsidytracker.user.dto;

import com.subsidytracker.common.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {
    private long id;
    private String email;
    private String fullName;
    private Role role;
    private String region;
}
