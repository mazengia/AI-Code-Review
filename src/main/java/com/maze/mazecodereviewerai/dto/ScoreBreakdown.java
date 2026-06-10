package com.maze.mazecodereviewerai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─────────────────────────────────────────────────────────────────────────────
// REQUEST
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// RESPONSE
// ─────────────────────────────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdown {
    private int readability;
    private int performance;
    private int security;
    private int maintainability;
    private int testability;
}

