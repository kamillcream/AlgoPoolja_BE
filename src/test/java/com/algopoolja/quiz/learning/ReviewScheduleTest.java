package com.algopoolja.quiz.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ReviewScheduleTest {
    @Test
    void usesSpacedReviewDelays() {
        OffsetDateTime attemptedAt = OffsetDateTime.parse("2026-09-20T10:00:00+09:00");

        assertThat(ReviewSchedule.nextReviewAt(attemptedAt, 1))
                .isEqualTo(OffsetDateTime.parse("2026-09-21T10:00:00+09:00"));
        assertThat(ReviewSchedule.nextReviewAt(attemptedAt, 2))
                .isEqualTo(OffsetDateTime.parse("2026-09-23T10:00:00+09:00"));
        assertThat(ReviewSchedule.nextReviewAt(attemptedAt, 3))
                .isEqualTo(OffsetDateTime.parse("2026-09-27T10:00:00+09:00"));
        assertThat(ReviewSchedule.nextReviewAt(attemptedAt, 4))
                .isEqualTo(OffsetDateTime.parse("2026-09-27T10:00:00+09:00"));
    }
}
