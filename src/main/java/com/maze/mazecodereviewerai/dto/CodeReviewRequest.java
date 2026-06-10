package com.maze.mazecodereviewerai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewRequest {

    @NotBlank(message = "Code must not be blank")
    @Size(max = 50000, message = "Code must be under 50 000 characters")
    private String code;

    @NotBlank(message = "Language must not be blank")
    private String language;          // java, python, typescript, sql, kotlin, go

    private String context;           // optional: "banking service", "payroll module"
    private boolean enableNlpPipeline = true;
    private boolean enableRefactoring = true;
    private String reviewDepth;       // QUICK | STANDARD | DEEP  (default: STANDARD)
}
