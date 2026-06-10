package com.enatbank.codereviewer.service;

import com.enatbank.codereviewer.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * ClaudeLlmService
 *
 * Responsible for:
 *  1. Building the structured system prompt with review depth and language context
 *  2. Calling the Anthropic /v1/messages endpoint via WebClient
 *  3. Parsing the JSON response into a ClaudeReviewResult
 *
 * The system prompt instructs Claude to respond ONLY with JSON so we can
 * deserialize it directly — same pattern used in the NLP pipeline Colab server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeLlmService {

    @Qualifier("claudeWebClient")
    private final WebClient claudeWebClient;

    private final ObjectMapper objectMapper;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    @Value("${claude.api.timeout-seconds}")
    private long timeoutSeconds;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are a senior software engineer performing a thorough code review.
        Language: %s
        Review depth: %s
        %s

        Analyze the provided code and respond ONLY with a single valid JSON object.
        No markdown fences, no preamble, no trailing explanation.

        Required JSON schema:
        {
          "overallScore": <integer 0-100>,
          "grade": <"A"|"B"|"C"|"D"|"F">,
          "verdict": <"one sentence verdict">,
          "scores": {
            "readability": <integer 0-100>,
            "performance": <integer 0-100>,
            "security": <integer 0-100>,
            "maintainability": <integer 0-100>,
            "testability": <integer 0-100>
          },
          "summary": <"3-4 sentence expert analysis">,
          "issues": [
            {
              "type": <"critical"|"warning"|"info"|"suggestion">,
              "title": <"short title">,
              "description": <"detailed explanation">,
              "line": <"Line X-Y or null">,
              "fix": <"corrected code snippet or null">,
              "category": <"security"|"performance"|"style"|"logic"|"null">
            }
          ],
          "refactoredCode": <"full refactored version of the code">,
          "refactorNotes": <"2-3 sentences explaining refactoring decisions">
        }

        Rules:
        - Be thorough. Find real issues specific to %s best practices.
        - Include Spring Boot / JPA / Angular patterns where relevant.
        - Security issues must be CRITICAL or WARNING.
        - The refactoredCode must be a complete, runnable replacement.
        - Respond with ONLY the JSON object.
        """;

    public ClaudeReviewResult review(String code, String language,
                                     String context, String depth) {
        String contextLine = (context != null && !context.isBlank())
            ? "Context: " + context
            : "";

        String systemPrompt = String.format(
            SYSTEM_PROMPT_TEMPLATE, language, depth, contextLine, language
        );

        ClaudeRequest request = ClaudeRequest.builder()
            .model(model)
            .max_tokens(maxTokens)
            .system(systemPrompt)
            .messages(List.of(
                ClaudeMessage.builder()
                    .role("user")
                    .content("Review this " + language + " code:\n\n" + code)
                    .build()
            ))
            .build();

        log.debug("Calling Claude API — model={} depth={} lang={}", model, depth, language);

        ClaudeResponse response = claudeWebClient.post()
            .uri("/v1/messages")
            .bodyValue(request)
            .retrieve()
            .onStatus(status -> status.is4xxClientError(), resp ->
                resp.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new RuntimeException("Claude 4xx: " + body)))
            )
            .onStatus(status -> status.is5xxServerError(), resp ->
                Mono.error(new RuntimeException("Claude API server error"))
            )
            .bodyToMono(ClaudeResponse.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .block();

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new RuntimeException("Empty response from Claude API");
        }

        String raw = response.getContent().stream()
            .filter(c -> "text".equals(c.getType()))
            .map(ClaudeResponse.ClaudeContent::getText)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No text content in Claude response"));

        // Strip accidental markdown fences
        String json = raw
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("```", "")
            .trim();

        try {
            return objectMapper.readValue(json, ClaudeReviewResult.class);
        } catch (Exception e) {
            log.error("Failed to parse Claude JSON response: {}", json, e);
            throw new RuntimeException("Claude response parse error: " + e.getMessage());
        }
    }
}
