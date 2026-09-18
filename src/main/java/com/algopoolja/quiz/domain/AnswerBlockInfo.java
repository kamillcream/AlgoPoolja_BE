package com.algopoolja.quiz.domain;

import java.util.List;

public record AnswerBlockInfo(
        List<String> correctAnswers,
        List<String> wrongAnswers
) {
}
