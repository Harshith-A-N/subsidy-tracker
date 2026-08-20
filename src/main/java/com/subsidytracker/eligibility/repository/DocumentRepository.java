package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplicationId(Long applicationId);
    List<Document> findByApplicationIdAndStageId(Long applicationId, Long stageId);
    boolean existsByApplicationIdAndStageId(Long applicationId, Long stageId);
}