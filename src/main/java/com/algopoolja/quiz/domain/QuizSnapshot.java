package com.algopoolja.quiz.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizSnapshot {
    private Long quizId;
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
        this(quizId, title, description, category, difficulty, testcases, answerCode, blankCode,
                correctAnswers, answerBlocks, null, null, null, null, null, List.of());
    }
}
