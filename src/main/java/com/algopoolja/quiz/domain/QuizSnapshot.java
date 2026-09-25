package com.algopoolja.quiz.domain;

import java.util.List;
import lombok.Getter;

@Getter
public class QuizSnapshot {
    private Long quizId;
    private String language;
    private String title;
    private String description;
    private String category;
    private String difficulty;
    private List<TestCase> testcases;
    private String answerCode;
    private String blankCode;
    private List<String> correctAnswers;
    private List<String> answerBlocks;
    private String hint;
    private String explanation;
    private String algorithm;
    private String timeComplexity;
    private String spaceComplexity;
    private List<String> commonMistakes;

    public QuizSnapshot(Long quizId, String title, String description, String category, String difficulty,
                        List<TestCase> testcases, String answerCode, String blankCode,
                        List<String> correctAnswers, List<String> answerBlocks) {
        this(quizId, "PYTHON", title, description, category, difficulty, testcases, answerCode, blankCode,
                correctAnswers, answerBlocks, null, null, null, null, null, List.of());
    }

    public QuizSnapshot(Long quizId, String title, String description, String category, String difficulty,
                        List<TestCase> testcases, String answerCode, String blankCode,
                        List<String> correctAnswers, List<String> answerBlocks, String hint, String explanation,
                        String algorithm, String timeComplexity, String spaceComplexity,
                        List<String> commonMistakes) {
        this(quizId, "PYTHON", title, description, category, difficulty, testcases, answerCode, blankCode,
                correctAnswers, answerBlocks, hint, explanation, algorithm, timeComplexity,
                spaceComplexity, commonMistakes);
    }

    public QuizSnapshot(Long quizId, String language, String title, String description, String category,
                        String difficulty, List<TestCase> testcases, String answerCode, String blankCode,
                        List<String> correctAnswers, List<String> answerBlocks, String hint, String explanation,
                        String algorithm, String timeComplexity, String spaceComplexity,
                        List<String> commonMistakes) {
        this.quizId = quizId;
        this.language = language;
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.testcases = testcases;
        this.answerCode = answerCode;
        this.blankCode = blankCode;
        this.correctAnswers = correctAnswers;
        this.answerBlocks = answerBlocks;
        this.hint = hint;
        this.explanation = explanation;
        this.algorithm = algorithm;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.commonMistakes = commonMistakes;
    }
}
