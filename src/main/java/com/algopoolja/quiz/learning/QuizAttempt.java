package com.algopoolja.quiz.learning;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizAttempt {
    private Long attemptId;
    private Long userId;
    private Long quizId;
    private List<String> selectedAnswers;
    private boolean correct;
    private int attemptNumber;
    private OffsetDateTime startedAt;
    private OffsetDateTime attemptedAt;

    public Long getDurationSeconds() {
        if (startedAt == null) return null;
        long seconds = Duration.between(startedAt, attemptedAt).getSeconds();
        return Math.max(0, seconds);
    }
}
