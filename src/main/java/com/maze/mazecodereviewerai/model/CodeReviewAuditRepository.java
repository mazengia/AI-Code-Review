package com.maze.mazecodereviewerai.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CodeReviewAuditRepository extends JpaRepository<CodeReviewAudit, String> {

    List<CodeReviewAudit> findTop20ByOrderByReviewedAtDesc();

    @Query("SELECT AVG(c.overallScore) FROM CodeReviewAudit c WHERE c.reviewedAt >= :since")
    Double findAverageScoreSince(LocalDateTime since);

    @Query("SELECT c.language, COUNT(c) FROM CodeReviewAudit c GROUP BY c.language ORDER BY COUNT(c) DESC")
    List<Object[]> countByLanguage();

    long countByReviewedAtAfter(LocalDateTime since);
}
