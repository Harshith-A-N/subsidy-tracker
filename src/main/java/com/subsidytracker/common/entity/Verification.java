package com.subsidytracker.common.entity;

import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.common.enums.VerificationLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Records a single verification action taken by an officer on an application.
 *
 * IMPORTANT: Each verification action creates a NEW record.
 * Previous records are NEVER overwritten — this maintains a complete approval history.
 *
 * One application can have many Verification records (e.g., field approve, district approve, etc.).
 */
@Entity
@Table(name = "verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The application being verified
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // The officer performing this verification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    // Stores the officer's role at the time of verification (e.g., "FIELD_OFFICER")
    @Column(name = "officer_role", nullable = false, length = 50)
    private String officerRole;

    // Which level of the workflow this verification belongs to
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationLevel level;

    // The officer's decision: APPROVED, REJECTED, or RE_VERIFICATION
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationDecision decision;

    // Officer's comments explaining the decision
    @Column(columnDefinition = "TEXT")
    private String remarks;

    // When this verification was performed
    @Column(name = "verification_date", nullable = false)
    private LocalDateTime verificationDate;

    // Automatically set the verification timestamp on creation
    @PrePersist
    protected void onCreate() {
        if (this.verificationDate == null) {
            this.verificationDate = LocalDateTime.now();
        }
    }
}
