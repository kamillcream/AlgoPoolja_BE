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
}
