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
public class FullQuizInfo {
    private String title;
    private String description;
    private List<TestCase> testcases;
    private String code;
    private String blankCode;
    private List<String> correctAnswers;
    private List<String> wrongAnswers;
    private String hint;
    private String explanation;
    private String algorithm;
    private String estimatedLevel;
    private String timeComplexity;
    private String spaceComplexity;
    private List<String> requiredTechniques;
    private List<String> commonMistakes;

    public ProblemInfo problem() {
        return new ProblemInfo(title, description, testcases, code, hint, explanation, algorithm,
                estimatedLevel, timeComplexity, spaceComplexity,
                requiredTechniques == null ? List.of() : requiredTechniques, commonMistakes);
    }

    public BlankQuizInfo blankQuiz() {
        return new BlankQuizInfo(blankCode, correctAnswers, wrongAnswers);
    }

    public FullQuizInfo(String title, String description, List<TestCase> testcases, String code,
                        String blankCode, List<String> correctAnswers, List<String> wrongAnswers) {
        this(title, description, testcases, code, blankCode, correctAnswers, wrongAnswers,
                null, null, null, null, List.of(), List.of());
    }
}
