package com.maze.mazecodereviewerai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeReviewResult {
    private int overallScore;
    private String grade;
    private String verdict;
    private ScoreBreakdown scores;
    private String summary;
    private List<CodeIssue> issues;
    private String refactoredCode;
    private String refactorNotes;
}
