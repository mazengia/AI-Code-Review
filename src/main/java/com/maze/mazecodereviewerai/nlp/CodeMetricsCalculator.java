package com.enatbank.codereviewer.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Local NLP pre-processing layer.
 * Computes lightweight code metrics before sending to the Colab NLP pipeline.
 * Mirrors what a @Component in a Spring microservice would do before
 * delegating heavy NLP work to a Python service.
 */
@Slf4j
@Component
public class CodeMetricsCalculator {

    private static final Pattern COMMENT_LINE_JAVA   = Pattern.compile("^\\s*(//|\\*)");
    private static final Pattern COMMENT_LINE_PYTHON = Pattern.compile("^\\s*#");
    private static final Pattern BLANK_LINE          = Pattern.compile("^\\s*$");

    // Cyclomatic complexity decision keywords per language
    private static final List<String> JAVA_DECISION_KW = List.of(
        "if ", "else if", "for ", "while ", "case ", "catch ", "&&", "||", "? "
    );
    private static final List<String> PYTHON_DECISION_KW = List.of(
        "if ", "elif ", "for ", "while ", "except ", "and ", "or "
    );
    private static final List<String> TS_DECISION_KW = List.of(
        "if ", "else if", "for ", "while ", "case ", "catch ", "&&", "||", "? "
    );

    // ── Public API ────────────────────────────────────────────────────────────

    public int computeLinesOfCode(String code) {
        return (int) Arrays.stream(code.split("\n"))
            .filter(l -> !BLANK_LINE.matcher(l).matches())
            .count();
    }

    public int computeCommentDensityPercent(String code, String language) {
        String[] lines = code.split("\n");
        long commentLines = Arrays.stream(lines)
            .filter(l -> isCommentLine(l, language))
            .count();
        long nonBlank = Arrays.stream(lines)
            .filter(l -> !BLANK_LINE.matcher(l).matches())
            .count();
        if (nonBlank == 0) return 0;
        return (int) Math.round((commentLines * 100.0) / nonBlank);
    }

    public int computeCyclomaticComplexity(String code, String language) {
        List<String> keywords = switch (language.toLowerCase()) {
            case "python" -> PYTHON_DECISION_KW;
            case "typescript", "javascript" -> TS_DECISION_KW;
            default -> JAVA_DECISION_KW;  // java, kotlin, go
        };
        int complexity = 1; // base
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = code.indexOf(kw, idx)) != -1) {
                complexity++;
                idx += kw.length();
            }
        }
        return complexity;
    }

    /**
     * Maintainability Index (SEI formula, clamped 0-100)
     * MI = 171 - 5.2*ln(HalsteadVolume) - 0.23*CC - 16.2*ln(LOC)
     * Simplified: we approximate Halstead volume as LOC * 4.
     */
    public double computeMaintainabilityIndex(int loc, int cc) {
        if (loc <= 0) return 100.0;
        double hv = Math.log(loc * 4.0 + 1);
        double mi = 171 - 5.2 * hv - 0.23 * cc - 16.2 * Math.log(loc + 1);
        mi = (mi / 171.0) * 100.0; // normalize to 0-100
        return Math.max(0, Math.min(100, Math.round(mi * 10.0) / 10.0));
    }

    public String classifyComplexity(int cc) {
        if (cc <= 5)  return "Low";
        if (cc <= 10) return "Medium";
        if (cc <= 20) return "High";
        return "Very High";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isCommentLine(String line, String language) {
        return switch (language.toLowerCase()) {
            case "python" -> COMMENT_LINE_PYTHON.matcher(line).find();
            default       -> COMMENT_LINE_JAVA.matcher(line).find();
        };
    }
}
