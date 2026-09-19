package com.algopoolja.quiz.api;

import static com.algopoolja.quiz.api.DashboardDtos.*;

import com.algopoolja.quiz.auth.CurrentUserProvider;
import com.algopoolja.quiz.learning.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/dashboard/summary")
    public DashboardResponse summary(
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(defaultValue = "5") int recentLimit,
            @RequestParam(defaultValue = "5") int wrongLimit,
            @RequestParam(defaultValue = "Asia/Seoul") String timezone,
            HttpServletRequest request) {
        return dashboardService.summary(currentUserProvider.requireCurrentUser(request), period,
                recentLimit, wrongLimit, timezone);
    }

    @GetMapping("/me/quiz-attempts")
    public PageResponse<AttemptView> attempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String result,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "attemptedAt,desc") String sort,
            @RequestParam(defaultValue = "Asia/Seoul") String timezone,
            HttpServletRequest request) {
        return dashboardService.attempts(currentUserProvider.requireCurrentUser(request), page, size,
                result, category, difficulty, from, to, sort, timezone);
    }

    @GetMapping("/me/wrong-quizzes")
    public PageResponse<WrongQuizView> wrongQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "unresolved") String status,
            @RequestParam(defaultValue = "lastAttemptedAt,desc") String sort,
            HttpServletRequest request) {
        return dashboardService.wrongQuizzes(currentUserProvider.requireCurrentUser(request), page, size,
                category, difficulty, status, sort);
    }
}
