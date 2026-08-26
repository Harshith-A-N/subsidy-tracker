package com.subsidytracker.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.enums.DocumentVerificationStatus;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplicationId(Long applicationId);
    List<Document> findByApplicationIdAndStageId(Long applicationId, Long stageId);
    boolean existsByApplicationIdAndStageId(Long applicationId, Long stageId);

    // KYC documents only (stage == null) - used to confirm the Field Officer
    // has actually verified every submitted document before an approval is
    // allowed to advance the application past FIELD review.
    List<Document> findByApplicationIdAndStageIsNull(Long applicationId);
    boolean existsByApplicationIdAndStageIsNullAndVerificationStatusNot(
            Long applicationId, DocumentVerificationStatus status);
}