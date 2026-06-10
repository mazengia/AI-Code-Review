package com.maze.mazecodereviewerai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeIssue {
    private String type;           // critical | warning | info | suggestion
    private String title;
    private String description;
    private String line;           // e.g. "Line 12-15"
    private String fix;            // suggested code fix snippet
    private String category;       // security | performance | style | logic
}
