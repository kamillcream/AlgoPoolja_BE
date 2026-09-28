package com.algopoolja.quiz.api;

import com.algopoolja.quiz.domain.TestCase;
import com.algopoolja.quiz.domain.ProblemInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class QuizDtos {
    private QuizDtos() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRequest {
        @NotBlank
        private String category;
        @NotBlank
        private String difficulty;
        @NotBlank
        private String language = "PYTHON";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateResponse {
        private Long quizId;
        private String language;
        private String title;
        private String description;
        private List<TestCase> testcases;
        private String blankCode;
        private List<String> answerBlocks;
        private int blankCount;
        private String answerCode;
        private String hint;
        private String explanation;
        private String algorithm;
        private String estimatedLevel;
        private String timeComplexity;
        private String spaceComplexity;
        private List<String> requiredTechniques;
        private List<String> commonMistakes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizDetailResponse {
        private Long quizId;
        private String language;
        private String title;
        private String description;
        private String category;
        private String difficulty;
        private List<TestCase> testcases;
        private String blankCode;
        private List<String> answerBlocks;
        private int blankCount;
        private String answerCode;
        private String hint;
        private String explanation;
        private String algorithm;
        private String timeComplexity;
        private String spaceComplexity;
        private List<String> commonMistakes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckRequest {
        @NotEmpty
        private List<@NotBlank String> answers;
        private OffsetDateTime startedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckResponse {
        private Long attemptId;
        private boolean correct;
        private String message;
        private int attemptNumber;
        private Long durationSeconds;
        private OffsetDateTime attemptedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private OffsetDateTime timestamp;
    }
}
