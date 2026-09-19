package com.algopoolja.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.ProblemInfo;
import com.algopoolja.quiz.domain.TestCase;
import com.algopoolja.quiz.learning.LearningStore;
import com.algopoolja.quiz.learning.UserIdentity;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuizServiceTest {
    @Test
    void generatesAndChecksQuiz() {
        AiQuizGenerator fake = new AiQuizGenerator() {
            public ProblemInfo createProblem(String c, String d) { return new ProblemInfo("합", "두 수의 합", List.of(new TestCase("입력: 1 2", "3"), new TestCase("2 3", "5")), "def solution(a, b):\n    return a + b"); }
            public String createBlankCode(String code) { return "def solution(a, b):\n    return <BLANK>"; }
            public AnswerBlockInfo createAnswerBlocks(String a, String b) { return new AnswerBlockInfo(List.of("a + b"), List.of("a - b", "a * b", "a / b")); }
        };
        QuizService service = new QuizService(fake, new QuizTools(), new LearningStore());
        var quiz = service.generate("전체", "쉬움");
        OffsetDateTime startedAt = OffsetDateTime.now().minusSeconds(184);
        var result = service.check(quiz.getQuizId(), List.of("a + b"), startedAt,
                new UserIdentity(12L, "알고풀자"));
        assertThat(quiz.getTestcases().getFirst().getInput()).isEqualTo("1 2");
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getAttemptId()).isEqualTo(1L);
        assertThat(result.getAttemptNumber()).isEqualTo(1);
        assertThat(result.getDurationSeconds()).isBetween(183L, 184L);
    }
}
