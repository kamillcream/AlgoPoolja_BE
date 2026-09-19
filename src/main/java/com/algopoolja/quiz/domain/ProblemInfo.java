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
}
