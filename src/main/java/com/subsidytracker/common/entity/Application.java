package com.subsidytracker.common.entity;

import com.subsidytracker.common.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="beneficiary_id", nullable = false)
    private Beneficiary beneficiary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="scheme_id", nullable = false)
    private Scheme scheme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private double eligibilityScore;

    @Column(nullable = false)
    private LocalDate submissionDate;

    // --- Module 2 additions (verification workflow support) ---

    // Tracks which verification desk currently holds the application
    // e.g., "FIELD_REVIEW", "DISTRICT_REVIEW", "FINANCE_REVIEW"
    @Column(name = "current_stage", length = 50)
    private String currentStage;

    // General remarks or notes attached to the application
    @Column(columnDefinition = "TEXT")
    private String remarks;

    // Comma-separated list of documents uploaded by the applicant
    @Column(name = "uploaded_documents", length = 1000)
    private String uploadedDocuments;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Automatically set timestamps when the record is first persisted
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Automatically update the timestamp on every modification
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
