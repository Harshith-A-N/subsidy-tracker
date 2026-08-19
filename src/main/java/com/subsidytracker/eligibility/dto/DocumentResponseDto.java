package com.subsidytracker.eligibility.dto;

import com.subsidytracker.common.enums.DocumentVerificationStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentResponseDto {
    private Long id;
    private Long applicationId;
    private String documentType;
    private String filePath;
    private LocalDateTime uploadedAt;
    private DocumentVerificationStatus verificationStatus;
    private String remarks;
}