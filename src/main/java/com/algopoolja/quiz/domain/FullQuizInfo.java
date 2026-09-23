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

    public ProblemInfo problem() {
        return new ProblemInfo(title, description, testcases, code);
    }

    public BlankQuizInfo blankQuiz() {
        return new BlankQuizInfo(blankCode, correctAnswers, wrongAnswers);
    }
}
