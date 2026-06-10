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
public class ClaudeRequest {
    private String model;
    private int max_tokens;
    private String system;
    private List<ClaudeMessage> messages;
}
