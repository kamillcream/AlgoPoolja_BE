package com.algopoolja.quiz.learning;

import static com.algopoolja.quiz.api.DashboardDtos.*;

import com.algopoolja.quiz.domain.QuizSnapshot;
import com.algopoolja.quiz.service.QuizService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final LearningStore store;
    private final QuizService quizService;

    public DashboardResponse summary(UserIdentity user, String periodType, int recentLimit,
                                     int wrongLimit, String timezone) {
        validateLimit(recentLimit, "recentLimit");
        validateLimit(wrongLimit, "wrongLimit");
        ZoneId zone = parseZone(timezone);
        PeriodRange range = period(periodType, zone);
        List<QuizAttempt> all = store.findAttempts(user.getUserId());
        List<QuizAttempt> selected = all.stream().filter(range::contains).toList();
        List<QuizAttempt> previous = all.stream().filter(range::containsPrevious).toList();

        return new DashboardResponse(
                new UserView(user.getUserId(), user.getNickname()),
                new PeriodView(periodType, range.getFrom(), range.getTo(), zone.getId()),
                buildSummary(selected, previous, all, zone),
                categoryStats(selected),
                wrongQuizzes(all, "unresolved", null, null).stream().limit(wrongLimit)
                        .map(this::summaryWrongQuizView).toList(),
                selected.stream().limit(recentLimit).map(this::recentAttemptView).toList());
    }

    public PageResponse<AttemptView> attempts(UserIdentity user, int page, int size, String result,
                                               String category, String difficulty, LocalDate from,
                                               LocalDate to, String sort, String timezone) {
        validatePage(page, size);
        validateResult(result);
        validateFilters(category, difficulty);
        validateSort(sort, "attemptedAt");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 이후일 수 없습니다.");
        }
        ZoneId zone = parseZone(timezone);
        Predicate<QuizAttempt> predicate = attempt -> matchesResult(attempt, result)
                && matchesQuiz(attempt, category, difficulty)
                && matchesDates(attempt, from, to, zone);
        List<QuizAttempt> filtered = store.findAttempts(user.getUserId()).stream().filter(predicate).toList();
        if (sort != null && sort.endsWith(",asc")) filtered = filtered.reversed();
        return page(filtered.stream().map(this::attemptView).toList(), page, size);
    }

    public PageResponse<WrongQuizView> wrongQuizzes(UserIdentity user, int page, int size, String category,
                                                    String difficulty, String status, String sort) {
        validatePage(page, size);
        validateWrongStatus(status);
        validateFilters(category, difficulty);
        validateSort(sort, "lastAttemptedAt");
        List<WrongQuizView> filtered = wrongQuizzes(store.findAttempts(user.getUserId()), status, category, difficulty);
        if (sort != null && sort.endsWith(",asc")) filtered = filtered.reversed();
        return page(filtered, page, size);
    }

    private SummaryView buildSummary(List<QuizAttempt> attempts, List<QuizAttempt> previous,
                                     List<QuizAttempt> all, ZoneId zone) {
        Map<Long, List<QuizAttempt>> byQuiz = groupByQuiz(attempts);
        long solved = byQuiz.size();
        long correct = byQuiz.values().stream().filter(list -> list.stream().anyMatch(QuizAttempt::isCorrect)).count();
        long wrong = solved - correct;
        double accuracy = accuracy(attempts);
        long weekly = all.stream()
                .filter(attempt -> !attempt.getAttemptedAt().atZoneSameInstant(zone).toLocalDate()
                        .isBefore(LocalDate.now(zone).minusDays(6)))
                .map(QuizAttempt::getQuizId).distinct().count();
        List<LocalDate> studyDays = all.stream()
                .map(attempt -> attempt.getAttemptedAt().atZoneSameInstant(zone).toLocalDate())
                .distinct().sorted().toList();
        Long seconds = attempts.stream().map(QuizAttempt::getDurationSeconds).filter(java.util.Objects::nonNull)
                .reduce(0L, Long::sum);
        Long studyMinutes = seconds == 0 && attempts.stream().noneMatch(a -> a.getDurationSeconds() != null)
                ? null : seconds / 60;
        return new SummaryView(solved, correct, wrong, accuracy, currentStreak(studyDays, zone),
                bestStreak(studyDays), studyMinutes, weekly,
                solved - groupByQuiz(previous).size(), round1(accuracy - accuracy(previous)));
    }

    private List<CategoryStatView> categoryStats(List<QuizAttempt> attempts) {
        Map<String, List<QuizAttempt>> grouped = attempts.stream().collect(Collectors.groupingBy(
                attempt -> quizService.getQuiz(attempt.getQuizId()).getCategory(), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> {
            Map<Long, List<QuizAttempt>> quizzes = groupByQuiz(entry.getValue());
            long correct = quizzes.values().stream().filter(list -> list.stream().anyMatch(QuizAttempt::isCorrect)).count();
            return new CategoryStatView(entry.getKey(), label(entry.getKey()), quizzes.size(), correct,
                    quizzes.size() - correct, accuracy(entry.getValue()));
        }).toList();
    }

    private List<WrongQuizView> wrongQuizzes(List<QuizAttempt> attempts, String status,
                                             String category, String difficulty) {
        List<WrongQuizView> result = new ArrayList<>();
        groupByQuiz(attempts).forEach((quizId, quizAttempts) -> {
            List<QuizAttempt> chronological = quizAttempts.stream()
                    .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt)).toList();
            QuizAttempt lastWrong = chronological.stream().filter(attempt -> !attempt.isCorrect())
                    .reduce((first, second) -> second).orElse(null);
            if (lastWrong == null) return;
            boolean resolved = chronological.stream().anyMatch(attempt -> attempt.isCorrect()
                    && attempt.getAttemptedAt().isAfter(lastWrong.getAttemptedAt()));
            if (!"all".equals(status) && ("resolved".equals(status) != resolved)) return;
            QuizSnapshot quiz = quizService.getQuiz(quizId);
            if (category != null && !category.equalsIgnoreCase(quiz.getCategory())) return;
            if (difficulty != null && !difficulty.equalsIgnoreCase(quiz.getDifficulty())) return;
            long wrongCount = chronological.stream().filter(attempt -> !attempt.isCorrect()).count();
            result.add(new WrongQuizView(quizId, quiz.getTitle(), quiz.getCategory(), quiz.getDifficulty(),
                    wrongCount, resolved, lastWrong.getAttemptedAt()));
        });
        return result.stream().sorted(Comparator.comparing(WrongQuizView::getLastAttemptedAt).reversed()).toList();
    }

    private AttemptView attemptView(QuizAttempt attempt) {
        QuizSnapshot quiz = quizService.getQuiz(attempt.getQuizId());
        return new AttemptView(attempt.getAttemptId(), attempt.getQuizId(), quiz.getTitle(), quiz.getCategory(),
                quiz.getDifficulty(), attempt.isCorrect(), attempt.getSelectedAnswers(), attempt.getAttemptNumber(),
                attempt.getDurationSeconds(), attempt.getAttemptedAt());
    }

    private RecentAttemptView recentAttemptView(QuizAttempt attempt) {
        AttemptView view = attemptView(attempt);
        return new RecentAttemptView(view.getAttemptId(), view.getQuizId(), view.getTitle(), view.getCategory(),
                view.getDifficulty(), view.isCorrect(), view.getAttemptNumber(), view.getDurationSeconds(),
                view.getAttemptedAt());
    }

    private SummaryWrongQuizView summaryWrongQuizView(WrongQuizView view) {
        return new SummaryWrongQuizView(view.getQuizId(), view.getTitle(), view.getCategory(), view.getDifficulty(),
                view.getWrongAttemptCount(), view.getLastAttemptedAt());
    }

    private boolean matchesQuiz(QuizAttempt attempt, String category, String difficulty) {
        QuizSnapshot quiz = quizService.getQuiz(attempt.getQuizId());
        return (category == null || category.equalsIgnoreCase(quiz.getCategory()))
                && (difficulty == null || difficulty.equalsIgnoreCase(quiz.getDifficulty()));
    }

    private boolean matchesDates(QuizAttempt attempt, LocalDate from, LocalDate to, ZoneId zone) {
        LocalDate date = attempt.getAttemptedAt().atZoneSameInstant(zone).toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private boolean matchesResult(QuizAttempt attempt, String result) {
        return "all".equals(result) || ("correct".equals(result) == attempt.isCorrect());
    }

    private Map<Long, List<QuizAttempt>> groupByQuiz(List<QuizAttempt> attempts) {
        return attempts.stream().collect(Collectors.groupingBy(QuizAttempt::getQuizId));
    }

    private double accuracy(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0.0;
        return round1(attempts.stream().filter(QuizAttempt::isCorrect).count() * 100.0 / attempts.size());
    }

    private double round1(double value) { return Math.round(value * 10.0) / 10.0; }

    private int currentStreak(List<LocalDate> days, ZoneId zone) {
        if (days.isEmpty()) return 0;
        Set<LocalDate> set = new LinkedHashSet<>(days);
        LocalDate cursor = LocalDate.now(zone);
        if (!set.contains(cursor)) cursor = cursor.minusDays(1);
        int streak = 0;
        while (set.contains(cursor)) { streak++; cursor = cursor.minusDays(1); }
        return streak;
    }

    private int bestStreak(List<LocalDate> days) {
        int best = 0, current = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            current = previous != null && day.equals(previous.plusDays(1)) ? current + 1 : 1;
            best = Math.max(best, current);
            previous = day;
        }
        return best;
    }

    private String label(String category) {
        return category.substring(0, 1) + category.substring(1).toLowerCase();
    }

    private PeriodRange period(String type, ZoneId zone) {
        LocalDate to = LocalDate.now(zone);
        return switch (type) {
            case "7d" -> new PeriodRange(to.minusDays(6), to, 7, zone);
            case "30d" -> new PeriodRange(to.minusDays(29), to, 30, zone);
            case "all" -> new PeriodRange(null, to, 0, zone);
            default -> throw new IllegalArgumentException("period는 7d, 30d, all 중 하나여야 합니다.");
        };
    }

    private ZoneId parseZone(String timezone) {
        try { return ZoneId.of(timezone); }
        catch (Exception exception) { throw new IllegalArgumentException("유효하지 않은 timezone입니다."); }
    }

    private void validateLimit(int value, String name) {
        if (value < 0 || value > 20) throw new IllegalArgumentException(name + "은 0 이상 20 이하여야 합니다.");
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page는 0 이상, size는 1 이상 100 이하여야 합니다.");
    }

    private void validateResult(String result) {
        if (!Set.of("correct", "wrong", "all").contains(result)) throw new IllegalArgumentException("유효하지 않은 result입니다.");
    }

    private void validateWrongStatus(String status) {
        if (!Set.of("unresolved", "resolved", "all").contains(status)) throw new IllegalArgumentException("유효하지 않은 status입니다.");
    }

    private void validateFilters(String category, String difficulty) {
        if (category != null && !Set.of("GREEDY", "DP", "BFS", "DFS", "ETC").contains(category.toUpperCase())) {
            throw new IllegalArgumentException("유효하지 않은 category입니다.");
        }
        if (difficulty != null && !Set.of("EASY", "NORMAL", "HARD").contains(difficulty.toUpperCase())) {
            throw new IllegalArgumentException("유효하지 않은 difficulty입니다.");
        }
    }

    private void validateSort(String sort, String field) {
        if (sort == null) return;
        if (!sort.equals(field + ",desc") && !sort.equals(field + ",asc")) throw new IllegalArgumentException("유효하지 않은 sort입니다.");
    }

    private <T> PageResponse<T> page(List<T> values, int page, int size) {
        int from = (int) Math.min((long) page * size, values.size());
        int to = Math.min(from + size, values.size());
        int totalPages = values.isEmpty() ? 0 : (values.size() + size - 1) / size;
        return new PageResponse<>(values.subList(from, to), page, size, values.size(), totalPages, page + 1 >= totalPages);
    }

    @Value
    private static class PeriodRange {
        LocalDate from;
        LocalDate to;
        int days;
        ZoneId zone;

        boolean contains(QuizAttempt attempt) {
            LocalDate date = attempt.getAttemptedAt().atZoneSameInstant(zone).toLocalDate();
            return from == null || (!date.isBefore(from) && !date.isAfter(to));
        }

        boolean containsPrevious(QuizAttempt attempt) {
            if (from == null) return false;
            LocalDate date = attempt.getAttemptedAt().atZoneSameInstant(zone).toLocalDate();
            return !date.isBefore(from.minusDays(days)) && date.isBefore(from);
        }
    }
}
