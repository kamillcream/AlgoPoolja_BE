package com.algopoolja.quiz.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.ProblemInfo;
import com.algopoolja.quiz.domain.TestCase;
import com.algopoolja.quiz.service.AiQuizGenerator;
import com.algopoolja.quiz.service.QuizService;
import com.algopoolja.quiz.service.QuizTools;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    @Test
    void aggregatesAttemptsAndTracksResolvedWrongQuiz() {
        LearningStore store = new LearningStore();
        QuizService quizService = new QuizService(fakeGenerator(), new QuizTools(), store);
        DashboardService dashboard = new DashboardService(store, quizService);
        UserIdentity user = new UserIdentity(12L, "알고풀자");
        Long quizId = quizService.generate("DP", "어려움").getQuizId();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));

        store.saveAttempt(user, quizId, List.of("a - b"), false, now.minusMinutes(5), now.minusMinutes(4));
        store.saveAttempt(user, quizId, List.of("a + b"), true, now.minusMinutes(2), now);

        var summary = dashboard.summary(user, "30d", 5, 5, "Asia/Seoul");
        var attempts = dashboard.attempts(user, 0, 20, "all", null, null,
                null, null, "attemptedAt,desc", "Asia/Seoul");
        var unresolved = dashboard.wrongQuizzes(user, 0, 20, null, null,
                "unresolved", "lastAttemptedAt,desc");
        var resolved = dashboard.wrongQuizzes(user, 0, 20, null, null,
                "resolved", "lastAttemptedAt,desc");

        assertThat(summary.getSummary().getSolvedCount()).isEqualTo(1);
        assertThat(summary.getSummary().getCorrectCount()).isEqualTo(1);
        assertThat(summary.getSummary().getAccuracyRate()).isEqualTo(50.0);
        assertThat(attempts.getTotalElements()).isEqualTo(2);
        assertThat(attempts.getContent().getFirst().isCorrect()).isTrue();
        assertThat(unresolved.getContent()).isEmpty();
        assertThat(resolved.getContent()).singleElement().satisfies(item -> {
            assertThat(item.getWrongAttemptCount()).isEqualTo(1);
            assertThat(item.isResolved()).isTrue();
        });
    }

    private AiQuizGenerator fakeGenerator() {
        return new AiQuizGenerator() {
            public ProblemInfo createProblem(String category, String difficulty) {
                return new ProblemInfo("최소 동전 조합", "설명", List.of(
                        new TestCase("1", "1"), new TestCase("2", "2")),
                        "def solution(a, b):\n    return a + b");
            }
            public String createBlankCode(String code) { return "def solution(a, b):\n    return <BLANK>"; }
            public AnswerBlockInfo createAnswerBlocks(String answer, String blank) {
                return new AnswerBlockInfo(List.of("a + b"), List.of("a - b", "a * b", "a / b"));
            }
        };
    }
}
