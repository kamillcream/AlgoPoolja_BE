package com.algopoolja.quiz.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Value;

public final class DashboardDtos {
    private DashboardDtos() {}

    @Value
    public static class UserView { Long userId; String nickname; }

    @Value
    public static class PeriodView { String type; LocalDate from; LocalDate to; String timezone; }

    @Value
    public static class SummaryView {
        long solvedCount;
        long correctCount;
        long wrongCount;
        double accuracyRate;
        int currentStreakDays;
        int bestStreakDays;
        Long studyMinutes;
        long weeklySolvedCount;
        long solvedChange;
        double accuracyRateChange;
    }

    @Value
    public static class CategoryStatView {
        String category;
        String label;
        long attemptedCount;
        long correctCount;
        long wrongCount;
        double accuracyRate;
    }

    @Value
    public static class WrongQuizView {
        Long quizId;
        String title;
        String category;
        String difficulty;
        long wrongAttemptCount;
        boolean resolved;
        OffsetDateTime lastAttemptedAt;
    }

    @Value
    public static class SummaryWrongQuizView {
        Long quizId;
        String title;
        String category;
        String difficulty;
        long wrongAttemptCount;
        OffsetDateTime lastAttemptedAt;
    }

    @Value
    public static class AttemptView {
        Long attemptId;
        Long quizId;
        String title;
        String category;
        String difficulty;
        boolean correct;
        List<String> selectedAnswers;
        int attemptNumber;
        Long durationSeconds;
        OffsetDateTime attemptedAt;
    }

    @Value
    public static class RecentAttemptView {
        Long attemptId;
        Long quizId;
        String title;
        String category;
        String difficulty;
        boolean correct;
        int attemptNumber;
        Long durationSeconds;
        OffsetDateTime attemptedAt;
    }

    @Value
    public static class DashboardResponse {
        UserView user;
        PeriodView period;
        SummaryView summary;
        List<CategoryStatView> categoryStats;
        List<SummaryWrongQuizView> wrongQuizzes;
        List<RecentAttemptView> recentAttempts;
    }

    @Value
    public static class PageResponse<T> {
        List<T> content;
        int page;
        int size;
        long totalElements;
        int totalPages;
        boolean last;
    }
}
