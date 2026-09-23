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
    private String timeComplexity;
    private String spaceComplexity;
    private List<String> commonMistakes;

    public ProblemInfo(String title, String description, List<TestCase> testcases, String code) {
        this(title, description, testcases, code, null, null, null, null, null, List.of());
    }
}
