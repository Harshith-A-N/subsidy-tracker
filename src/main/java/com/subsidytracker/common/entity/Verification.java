package com.subsidytracker.common.entity;

import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.common.enums.VerificationLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "verifications")
@Getter
@Setter
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationDecision decision;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime verificationDate;

    @PrePersist
    protected void onCreate() {
        if (this.verificationDate == null) {
            this.verificationDate = LocalDateTime.now();
        }
    }
}