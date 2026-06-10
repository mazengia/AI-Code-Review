package com.maze.mazecodereviewerai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NlpAnalysis {
    // From Colab NLP pipeline
    private String complexity;         // Low | Medium | High | Very High
    private String sentiment;          // Positive | Neutral | Negative
    private String intent;             // e.g. "Data retrieval service"
    private List<String> keyphrases;
    private List<String> codeSmells;
    private List<String> designPatterns;
    private List<String> topics;

    // Metrics computed locally
    private int cyclomaticComplexity;
    private int linesOfCode;
    private int commentDensityPercent;
    private double maintainabilityIndex;
}
