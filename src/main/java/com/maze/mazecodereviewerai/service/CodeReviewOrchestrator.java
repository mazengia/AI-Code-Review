package com.maze.mazecodereviewerai.service;

import com.maze.mazecodereviewerai.dto.ClaudeReviewResult;
import com.maze.mazecodereviewerai.dto.CodeReviewRequest;
import com.maze.mazecodereviewerai.dto.CodeReviewResponse;
import com.maze.mazecodereviewerai.dto.NlpAnalysis;
import com.maze.mazecodereviewerai.model.CodeReviewAudit;
import com.maze.mazecodereviewerai.model.CodeReviewAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * CodeReviewOrchestrator
 *
 * The central pipeline coordinator. Runs the NLP stage and LLM stage
 * in parallel using a CompletableFuture fork-join, then merges results.
 *
 * Pipeline:
 *   Request ──▶ [NLP Analysis (Colab)]  ─┐
 *               [Claude LLM Review]     ─┴──▶ Merge ──▶ Audit Log ──▶ Response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewOrchestrator {

    private final ClaudeLlmService claudeLlmService;
    private final NlpPipelineService nlpPipelineService;
    private final CodeReviewAuditRepository auditRepository;

    public CodeReviewResponse orchestrate(CodeReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String reviewId = UUID.randomUUID().toString();
        String depth = Optional.ofNullable(request.getReviewDepth())
            .filter(s -> !s.isBlank())
            .orElse("STANDARD");

        log.info("Starting review [{}] lang={} depth={}", reviewId, request.getLanguage(), depth);

        String codeHash = sha256(request.getCode());
        Map<String, Object> pipelineStages = new LinkedHashMap<>();

        // ── Stage 1: NLP (parallel) ────────────────────────────────────────────
        long nlpStart = System.currentTimeMillis();
        CompletableFuture<NlpAnalysis> nlpFuture = request.isEnableNlpPipeline()
            ? CompletableFuture.supplyAsync(() ->
                nlpPipelineService.analyze(request.getCode(), request.getLanguage(), codeHash))
            : CompletableFuture.completedFuture(null);

        // ── Stage 2: Claude LLM (parallel) ────────────────────────────────────
        long claudeStart = System.currentTimeMillis();
        CompletableFuture<ClaudeReviewResult> claudeFuture =
            CompletableFuture.supplyAsync(() ->
                claudeLlmService.review(
                    request.getCode(),
                    request.getLanguage(),
                    request.getContext(),
                    depth
                ));

        // ── Join both stages ──────────────────────────────────────────────────
        NlpAnalysis nlpAnalysis = null;
        ClaudeReviewResult claudeResult;

        try {
            nlpAnalysis = nlpFuture.get(35, TimeUnit.SECONDS);
            pipelineStages.put("nlp_pipeline_ms", System.currentTimeMillis() - nlpStart);
        } catch (Exception e) {
            log.warn("NLP stage failed or timed out: {}", e.getMessage());
            pipelineStages.put("nlp_pipeline_ms", -1);
        }

        try {
            claudeResult = claudeFuture.get(65, TimeUnit.SECONDS);
            pipelineStages.put("llm_analysis_ms", System.currentTimeMillis() - claudeStart);
        } catch (Exception e) {
            log.error("Claude LLM stage failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM analysis failed: " + e.getMessage());
        }

        long totalMs = System.currentTimeMillis() - startTime;
        pipelineStages.put("total_ms", totalMs);

        // ── Merge & build response ────────────────────────────────────────────
        CodeReviewResponse response = CodeReviewResponse.builder()
            .reviewId(reviewId)
            .reviewedAt(LocalDateTime.now())
            .language(request.getLanguage())
            .overallScore(claudeResult.getOverallScore())
            .grade(claudeResult.getGrade())
            .verdict(claudeResult.getVerdict())
            .scores(claudeResult.getScores())
            .summary(claudeResult.getSummary())
            .issues(claudeResult.getIssues())
            .nlpAnalysis(nlpAnalysis)
            .refactoredCode(request.isEnableRefactoring() ? claudeResult.getRefactoredCode() : null)
            .refactorNotes(request.isEnableRefactoring() ? claudeResult.getRefactorNotes() : null)
            .processingTimeMs(totalMs)
            .modelUsed(System.getProperty("claude.model", "claude-opus-4-6"))
            .pipelineStages(pipelineStages)
            .build();

        // ── Audit log ─────────────────────────────────────────────────────────
        saveAudit(response, request, codeHash, nlpAnalysis);
        log.info("Review [{}] complete — score={} grade={} issues={} time={}ms",
            reviewId, response.getOverallScore(), response.getGrade(),
            Optional.ofNullable(response.getIssues()).map(List::size).orElse(0), totalMs);

        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveAudit(CodeReviewResponse r, CodeReviewRequest req,
                            String codeHash, NlpAnalysis nlp) {
        try {
            long criticals = Optional.ofNullable(r.getIssues()).orElse(List.of())
                .stream().filter(i -> "critical".equals(i.getType())).count();

            CodeReviewAudit audit = CodeReviewAudit.builder()
                .language(r.getLanguage())
                .codeSnippetHash(codeHash)
                .overallScore(r.getOverallScore())
                .grade(r.getGrade())
                .issueCount(Optional.ofNullable(r.getIssues()).map(List::size).orElse(0))
                .criticalCount((int) criticals)
                .complexity(nlp != null ? nlp.getComplexity() : null)
                .processingTimeMs(r.getProcessingTimeMs())
                .nlpEnabled(req.isEnableNlpPipeline())
                .modelUsed(r.getModelUsed())
                .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.warn("Audit log save failed (non-fatal): {}", e.getMessage());
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
