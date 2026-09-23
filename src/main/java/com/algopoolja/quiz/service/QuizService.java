package com.algopoolja.quiz.service;

import com.algopoolja.quiz.api.QuizDtos.CheckResponse;
import com.algopoolja.quiz.api.QuizDtos.GenerateResponse;
import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.BlankQuizInfo;
import com.algopoolja.quiz.domain.FullQuizInfo;
import com.algopoolja.quiz.domain.ProblemInfo;
import com.algopoolja.quiz.domain.TestCase;
import com.algopoolja.quiz.domain.QuizSnapshot;
import com.algopoolja.quiz.learning.LearningStore;
import com.algopoolja.quiz.learning.UserIdentity;
import com.algopoolja.quiz.persistence.QuizStore;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {
    private static final Set<String> CATEGORIES = Set.of("전체", "Greedy", "DP", "BFS", "DFS");
    private static final Set<String> DIFFICULTIES = Set.of("쉬움", "보통", "어려움");

    private final AiQuizGenerator generator;
    private final QuizTools tools;
    private final LearningStore learningStore;
    private final QuizStore quizStore;
    private final WrongAnswerGenerator wrongAnswerGenerator = new WrongAnswerGenerator();

    @Value("${app.ai.generation-strategy:two-step}")
    private String generationStrategy;

    public GenerateResponse generate(String category, String difficulty) {
        long startedAt = System.nanoTime();
        if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("지원하지 않는 문제 유형입니다.");
        if (!DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException("지원하지 않는 문제 난이도입니다.");

        ProblemInfo problem;
        BlankQuizInfo blankQuiz;
        if ("one-step".equalsIgnoreCase(generationStrategy)) {
            FullQuizInfo fullQuiz = generator.createFullQuiz(category, difficulty);
            problem = fullQuiz.problem();
            blankQuiz = fullQuiz.blankQuiz();
        } else if ("parallel-pipeline".equalsIgnoreCase(generationStrategy)) {
            problem = generator.createProblem(category, difficulty);
            blankQuiz = generateParallelPipeline(problem);
        } else {
            problem = generator.createProblem(category, difficulty);
            blankQuiz = "legacy".equalsIgnoreCase(generationStrategy)
                    ? new BlankQuizInfo(generator.createBlankCode(problem.getCode()), null, null)
                    : generator.createBlankQuiz(problem.getCode());
            if ("legacy".equalsIgnoreCase(generationStrategy)) {
                AnswerBlockInfo blocks = generator.createAnswerBlocks(problem.getCode(), blankQuiz.getBlankCode());
                blankQuiz.setCorrectAnswers(blocks.getCorrectAnswers());
                blankQuiz.setWrongAnswers(blocks.getWrongAnswers());
            }
        }
        requireValidProblem(problem);
        String blankCode = blankQuiz.getBlankCode();
        AnswerBlockInfo blocks = new AnswerBlockInfo(blankQuiz.getCorrectAnswers(), blankQuiz.getWrongAnswers());
        String validation = tools.validateBlankCount(blankCode, blocks.getCorrectAnswers());
        if (!"OK".equals(validation)) throw new QuizGenerationException(validation);

        List<String> choices = tools.buildChoiceBlocks(blocks.getCorrectAnswers(), blocks.getWrongAnswers());
        List<TestCase> testCases = problem.getTestcases().stream()
                .map(test -> new TestCase(tools.cleanTestcaseText(test.getInput()), tools.cleanTestcaseText(test.getOutput())))
                .toList();

        long id = quizStore.save(problem.getTitle(), problem.getDescription(), normalizeCategory(category),
                difficulty, problem.getCode(), blankCode, blocks.getCorrectAnswers(), choices, testCases);
        log.info("Quiz generation completed: strategy={}, totalMs={}", generationStrategy, elapsedMillis(startedAt));
        return new GenerateResponse(id, problem.getTitle(), problem.getDescription(), testCases, blankCode,
                choices, blocks.getCorrectAnswers().size(), problem.getCode());
    }

    private BlankQuizInfo generateParallelPipeline(ProblemInfo problem) {
        long startedAt = System.nanoTime();
        CompletableFuture<String> blankCodeFuture = CompletableFuture.supplyAsync(
                () -> generator.createBlankCode(problem.getCode()));
        CompletableFuture<List<TestCase>> cleanedTestsFuture = CompletableFuture.supplyAsync(
                () -> problem.getTestcases().stream()
                        .map(test -> new TestCase(tools.cleanTestcaseText(test.getInput()),
                                tools.cleanTestcaseText(test.getOutput())))
                        .toList());

        String blankCode = blankCodeFuture.join();
        List<String> correctAnswers = extractCorrectAnswers(problem.getCode(), blankCode);
        List<String> wrongAnswers = wrongAnswerGenerator.generate(correctAnswers);
        // Join the independent branch before returning so validation observes the same snapshot.
        cleanedTestsFuture.join();
        log.info("Quiz pipeline completed: stage=blank-and-clean, totalMs={}", elapsedMillis(startedAt));
        return new BlankQuizInfo(blankCode, correctAnswers, wrongAnswers);
    }

    /** Extracts blank contents without another AI call and verifies reconstruction. */
    private List<String> extractCorrectAnswers(String answerCode, String blankCode) {
        List<String> answers = new java.util.ArrayList<>();
        int sourceIndex = 0;
        int blankIndex = 0;
        while (blankIndex < blankCode.length()) {
            int marker = blankCode.indexOf("<BLANK>", blankIndex);
            if (marker < 0) {
                if (!answerCode.substring(sourceIndex).equals(blankCode.substring(blankIndex))) {
                    throw new QuizGenerationException("빈칸 코드가 원본 정답 코드와 일치하지 않습니다.");
                }
                sourceIndex = answerCode.length();
                blankIndex = blankCode.length();
                break;
            }
            String literal = blankCode.substring(blankIndex, marker);
            if (!answerCode.startsWith(literal, sourceIndex)) {
                throw new QuizGenerationException("빈칸 코드의 원본 위치를 확인할 수 없습니다.");
            }
            sourceIndex += literal.length();
            int nextMarker = blankCode.indexOf("<BLANK>", marker + 7);
            int nextLiteralStart = nextMarker < 0 ? blankCode.length() : nextMarker;
            String suffix = blankCode.substring(marker + 7, nextLiteralStart);
            int suffixIndex = suffix.isEmpty() ? answerCode.length() : answerCode.indexOf(suffix, sourceIndex);
            if (suffixIndex < sourceIndex) {
                throw new QuizGenerationException("빈칸 코드의 정답 블록을 추출할 수 없습니다.");
            }
            answers.add(answerCode.substring(sourceIndex, suffixIndex));
            sourceIndex = suffixIndex;
            blankIndex = marker + 7;
        }
        if (sourceIndex != answerCode.length()) {
            throw new QuizGenerationException("빈칸 코드로 정답 코드를 복원할 수 없습니다.");
        }
        return answers;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    public CheckResponse check(Long id, List<String> answers, OffsetDateTime startedAt, UserIdentity user) {
        QuizSnapshot quiz = getQuiz(id);
        if (answers.size() != quiz.getCorrectAnswers().size()) {
            throw new IllegalArgumentException("빈칸 %d개를 모두 선택해주세요.".formatted(quiz.getCorrectAnswers().size()));
        }
        String completed = quiz.getBlankCode();
        for (String answer : answers) {
            completed = completed.replaceFirst("<BLANK>", java.util.regex.Matcher.quoteReplacement(answer));
        }
        boolean correct = answers.equals(quiz.getCorrectAnswers()) || completed.strip().equals(quiz.getAnswerCode().strip());
        var attempt = learningStore.saveAttempt(user, id, answers, correct, startedAt,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        return new CheckResponse(attempt.getAttemptId(), correct,
                correct ? "정답입니다." : "다시 확인해보세요.", attempt.getAttemptNumber(),
                attempt.getDurationSeconds(), attempt.getAttemptedAt());
    }

    public QuizSnapshot getQuiz(Long id) {
        return quizStore.findById(id).orElseThrow(QuizNotFoundException::new);
    }

    private String normalizeCategory(String value) {
        return "전체".equals(value) ? "ETC" : value.toUpperCase();
    }

    private void requireValidProblem(ProblemInfo problem) {
        if (problem == null || problem.getTitle() == null || problem.getDescription() == null || problem.getCode() == null
                || problem.getTestcases() == null || problem.getTestcases().size() < 2) {
            throw new QuizGenerationException("AI가 유효한 문제 또는 테스트케이스를 생성하지 못했습니다.");
        }
    }

}
