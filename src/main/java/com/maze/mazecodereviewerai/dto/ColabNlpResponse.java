package com.maze.mazecodereviewerai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColabNlpResponse {
    private String complexity;
    private String sentiment;
    private String intent;
    private List<String> keyphrases;
    private List<String> codeSmells;
    private List<String> designPatterns;
    private List<String> topics;
    private int cyclomaticComplexity;
    private int linesOfCode;
    private int commentDensityPercent;
    private double maintainabilityIndex;
}
