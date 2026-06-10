package com.enatbank.codereviewer.controller;

import com.enatbank.codereviewer.dto.CodeReviewRequest;
import com.enatbank.codereviewer.dto.CodeReviewResponse;
import com.enatbank.codereviewer.model.CodeReviewAuditRepository;
import com.enatbank.codereviewer.service.CodeReviewOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/code-review")
@RequiredArgsConstructor
@Tag(name = "Code Review", description = "AI-powered code review with Claude LLM + NLP pipeline")
public class CodeReviewController {

    private final CodeReviewOrchestrator orchestrator;
    private final CodeReviewAuditRepository auditRepository;

    // ─── POST /api/v1/code-review ─────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Submit code for AI review",
        description = "Runs the full NLP pipeline (Google Colab) + Claude LLM analysis in parallel. " +
                      "Returns scores, issues, NLP analysis, and a refactored version."
    )
    public ResponseEntity<CodeReviewResponse> review(
            @Valid @RequestBody CodeReviewRequest request) {

        long start = System.currentTimeMillis();
        CodeReviewResponse response = orchestrator.orchestrate(request);
        long elapsed = System.currentTimeMillis() - start;

        return ResponseEntity.ok()
            .header("X-Review-Id", response.getReviewId())
            .header("X-Processing-Time", elapsed + "ms")
            .body(response);
    }

    // ─── GET /api/v1/code-review/stats ───────────────────────────────────────

    @GetMapping("/stats")
    @Operation(summary = "Review statistics", description = "Returns aggregate stats from the audit log")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReviews", auditRepository.count());
        stats.put("reviewsLast24h",
            auditRepository.countByReviewedAtAfter(LocalDateTime.now().minusHours(24)));
        stats.put("averageScoreLast7Days",
            auditRepository.findAverageScoreSince(LocalDateTime.now().minusDays(7)));
        stats.put("reviewsByLanguage",
            auditRepository.countByLanguage().stream()
                .map(row -> Map.of("language", row[0], "count", row[1]))
                .toList());
        stats.put("recentReviews",
            auditRepository.findTop20ByOrderByReviewedAtDesc());
        return ResponseEntity.ok(stats);
    }

    // ─── GET /api/v1/code-review/health ──────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "AI Code Reviewer",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
