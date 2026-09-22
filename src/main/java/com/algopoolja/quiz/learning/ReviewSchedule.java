package com.algopoolja.quiz.learning;

import java.time.OffsetDateTime;

public final class ReviewSchedule {
    private ReviewSchedule() {}

    public static OffsetDateTime nextReviewAt(OffsetDateTime attemptedAt, int wrongCount) {
        return attemptedAt.plusDays(delayDays(wrongCount));
    }

    public static int delayDays(int wrongCount) {
        if (wrongCount <= 1) return 1;
        if (wrongCount == 2) return 3;
        return 7;
    }
}
