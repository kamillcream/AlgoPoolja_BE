package com.algopoolja.quiz.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AnswerBlockInfo(
        @JsonProperty("correct_answers") List<String> correctAnswers,
        @JsonProperty("wrong_answers") List<String> wrongAnswers
) {
}
