package com.maze.mazecodereviewerai.service;

import com.maze.mazecodereviewerai.dto.ColabNlpRequest;
import com.maze.mazecodereviewerai.dto.ColabNlpResponse;
import com.maze.mazecodereviewerai.dto.NlpAnalysis;
import com.maze.mazecodereviewerai.nlp.CodeMetricsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;

/**
 * NlpPipelineService
 *
 * Calls the Google Colab Flask/ngrok NLP server for:
 *  - Keyphrase extraction (KeyBERT)
 *  - Sentiment analysis (transformers)
 *  - Topic classification (zero-shot)
 *  - Code smell detection
 *
 * Falls back to local metric computation if Colab is unreachable
 * (e.g. during development or when ngrok session expires).
 *
 * Results are cached by (code hash + language) for 30 minutes
 * via Caffeine cache defined in application.yml.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NlpPipelineService {

    @Qualifier("colabWebClient")
    private final WebClient colabWebClient;

    private final CodeMetricsCalculator metricsCalculator;

    @Value("${nlp.pipeline.colab.timeout-seconds}")
    private long colabTimeoutSeconds;

    @Value("${nlp.pipeline.enabled}")
    private boolean nlpEnabled;

    /**
     * Main entry point. Calls Colab for NLP; enriches with local metrics.
     * Cached by sha256 hash of the code to avoid redundant calls.
     */
    @Cacheable(value = "nlpResults", key = "#codeHash + '_' + #language")
    public NlpAnalysis analyze(String code, String language, String codeHash) {
        int loc = metricsCalculator.computeLinesOfCode(code);
        int cc  = metricsCalculator.computeCyclomaticComplexity(code, language);
        int commentDensity = metricsCalculator.computeCommentDensityPercent(code, language);
        double mi = metricsCalculator.computeMaintainabilityIndex(loc, cc);
        String complexityLabel = metricsCalculator.classifyComplexity(cc);

        if (!nlpEnabled) {
            log.info("NLP pipeline disabled — returning local metrics only");
            return buildLocalOnly(loc, cc, commentDensity, mi, complexityLabel);
        }

        try {
            ColabNlpRequest req = ColabNlpRequest.builder()
                .code(code)
                .language(language)
                .build();

            ColabNlpResponse colabResult = colabWebClient.post()
                .uri("/analyze")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ColabNlpResponse.class)
                .timeout(Duration.ofSeconds(colabTimeoutSeconds))
                .block();

            if (colabResult == null) {
                log.warn("Colab NLP returned null — falling back to local");
                return buildLocalOnly(loc, cc, commentDensity, mi, complexityLabel);
            }

            // Merge: Colab linguistic analysis + local metrics
            return NlpAnalysis.builder()
                .complexity(complexityLabel)
                .sentiment(colabResult.getSentiment())
                .intent(colabResult.getIntent())
                .keyphrases(colabResult.getKeyphrases())
                .codeSmells(colabResult.getCodeSmells())
                .designPatterns(colabResult.getDesignPatterns())
                .topics(colabResult.getTopics())
                .cyclomaticComplexity(cc)
                .linesOfCode(loc)
                .commentDensityPercent(commentDensity)
                .maintainabilityIndex(mi)
                .build();

        } catch (WebClientRequestException e) {
            log.warn("Colab NLP server unreachable ({}). Using local fallback.", e.getMessage());
            return buildLocalOnly(loc, cc, commentDensity, mi, complexityLabel);
        } catch (Exception e) {
            log.error("NLP pipeline error: {}", e.getMessage(), e);
            return buildLocalOnly(loc, cc, commentDensity, mi, complexityLabel);
        }
    }

    private NlpAnalysis buildLocalOnly(int loc, int cc, int commentDensity,
                                        double mi, String complexityLabel) {
        return NlpAnalysis.builder()
            .complexity(complexityLabel)
            .sentiment("Neutral")
            .intent("Unknown (Colab offline)")
            .cyclomaticComplexity(cc)
            .linesOfCode(loc)
            .commentDensityPercent(commentDensity)
            .maintainabilityIndex(mi)
            .build();
    }
}
