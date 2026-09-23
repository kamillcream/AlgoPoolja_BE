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
