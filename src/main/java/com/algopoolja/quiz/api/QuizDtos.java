package com.algopoolja.quiz.api;

import com.algopoolja.quiz.domain.TestCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public final class QuizDtos {
    private QuizDtos() {}

    public record GenerateRequest(@NotBlank String category, @NotBlank String difficulty) {}

    public record GenerateResponse(
            Long quizId,
            String title,
            String description,
            List<TestCase> testcases,
            String blankCode,
            List<String> answerBlocks,
            int blankCount,
            String answerCode
    ) {}

    public record CheckRequest(@NotEmpty List<@NotBlank String> answers) {}

    public record CheckResponse(boolean correct, String message, String completedCode) {}

    public record ErrorResponse(String message) {}
}
