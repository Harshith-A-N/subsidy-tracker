package com.subsidytracker.security.dto;

import com.subsidytracker.common.enums.RequestStatus;
import com.subsidytracker.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfficerRegistrationResponseDto {
    private long id;
    private String fullName;
    private String email;
    private String phone;
    private Role requestedRole;
    private String region;
    private RequestStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByEmail;
    private String rejectionReason;
}
