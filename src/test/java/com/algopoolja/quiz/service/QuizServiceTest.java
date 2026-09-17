package com.algopoolja.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.ProblemInfo;
import com.algopoolja.quiz.domain.TestCase;
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
        QuizService service = new QuizService(fake, new QuizTools());
        var quiz = service.generate("전체", "쉬움");
        var result = service.check(quiz.quizId(), List.of("a + b"));
        assertThat(quiz.testcases().getFirst().input()).isEqualTo("1 2");
        assertThat(result.correct()).isTrue();
        assertThat(result.completedCode()).isEqualTo("def solution(a, b):\n    return a + b");
    }
}
