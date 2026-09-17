package com.algopoolja.quiz.domain;

import java.util.List;

public record ProblemInfo(String title, String description, List<TestCase> testcases, String code) {
}
