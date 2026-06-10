package com.maze.mazecodereviewerai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeReviewResponse {

    private String reviewId;
    private LocalDateTime reviewedAt;
    private String language;

    // Scores
    private int overallScore;          // 0-100
    private String grade;              // A B C D F
    private String verdict;
    private ScoreBreakdown scores;

    // Narrative
    private String summary;
    private List<CodeIssue> issues;

    // NLP layer
    private NlpAnalysis nlpAnalysis;

    // Refactored output
    private String refactoredCode;
    private String refactorNotes;

    // Meta
    private long processingTimeMs;
    private String modelUsed;
    private Map<String, Object> pipelineStages; // stage → duration ms
}
