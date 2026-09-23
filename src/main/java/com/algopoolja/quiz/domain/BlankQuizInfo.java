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
public class BlankQuizInfo {
    private String blankCode;
    private List<String> correctAnswers;
    private List<String> wrongAnswers;
}
