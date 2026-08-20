package com.subsidytracker.common.entity;

import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private DisbursementStage stage;

    @Column(nullable = false)
    private String documentType; // must match a value in Scheme.requiredDocuments

    @Column(nullable = false)
    private String filePath; // where the actual file is stored on disk

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentVerificationStatus verificationStatus;

    private String remarks; // field officer's note, e.g. why rejected
}