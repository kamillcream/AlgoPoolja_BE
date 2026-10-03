package com.algopoolja.quiz.learning;

public record WeeklyRanking(
        Long userId,
        String nickname,
        long solvedCount,
        long correctCount,
        long attemptCount) {
}
