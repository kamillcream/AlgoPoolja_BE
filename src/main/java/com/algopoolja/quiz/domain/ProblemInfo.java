package com.algopoolja.quiz.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemInfo {
    private String title;
    private String description;
    private List<TestCase> testcases;
    private String code;
    private String hint;
    private String explanation;
    private String algorithm;
    private String estimatedLevel;
    private String timeComplexity;
    private String spaceComplexity;
    private List<String> requiredTechniques;
    private List<String> commonMistakes;

    public ProblemInfo(String title, String description, List<TestCase> testcases, String code) {
        this(title, description, testcases, code, null, null, "DP", "프로그래머스 Lv.2",
                "O(N)", "O(1)", List.of("동적 계획법"), List.of());
    }

    public ProblemInfo(String title, String description, List<TestCase> testcases, String code,
                       DifficultyMetadata metadata) {
        this(title, description, testcases, code, null, null,
                metadata == null ? null : metadata.algorithm(),
                metadata == null ? null : metadata.estimatedLevel(),
                metadata == null ? null : metadata.timeComplexity(),
                metadata == null ? null : metadata.spaceComplexity(),
                metadata == null ? List.of() : metadata.requiredTechniques(), List.of());
    }
}
