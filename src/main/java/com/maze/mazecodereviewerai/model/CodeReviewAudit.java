package com.enatbank.codereviewer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_review_audit", indexes = {
    @Index(name = "idx_language", columnList = "language"),
    @Index(name = "idx_reviewed_at", columnList = "reviewedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String codeSnippetHash;   // SHA-256 hash – never store raw code

    @Column(nullable = false)
    private int overallScore;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private int issueCount;

    @Column(nullable = false)
    private int criticalCount;

    private String complexity;
    private long processingTimeMs;

    @CreationTimestamp
    private LocalDateTime reviewedAt;

    @Column(name = "nlp_enabled")
    private boolean nlpEnabled;

    @Column(name = "model_used")
    private String modelUsed;
}
