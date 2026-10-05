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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {
    private static final Set<String> CATEGORIES = Set.of(
            "전체", "Greedy", "DP", "BFS", "DFS", "구현/시뮬레이션", "스택/큐",
            "해시/정렬", "이분 탐색", "힙/우선순위 큐", "완전탐색/백트래킹", "그래프", "문자열/파싱");
    private static final Set<String> DIFFICULTIES = Set.of("쉬움", "보통", "어려움");
    private static final Set<String> LANGUAGES = Set.of("PYTHON", "JAVASCRIPT", "JAVA", "CPP");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("(?:LV\\.?\\s*)?(\\d+)", Pattern.CASE_INSENSITIVE);

    private final AiQuizGenerator generator;
    private final QuizTools tools;
    private final LearningStore learningStore;
    private final QuizStore quizStore;
    private final WrongAnswerGenerator wrongAnswerGenerator = new WrongAnswerGenerator();

    @Value("${app.ai.generation-strategy:two-step}")
    private String generationStrategy;

    public GenerateResponse generate(String category, String difficulty) {
        return generate(category, difficulty, "PYTHON", null);
    }

    public GenerateResponse generate(String category, String difficulty, String language) {
        return generate(category, difficulty, language, null);
    }

    public GenerateResponse generate(String category, String difficulty, String language, UserIdentity user) {
        long startedAt = System.nanoTime();
        if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("지원하지 않는 문제 유형입니다.");
        if (!DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException("지원하지 않는 문제 난이도입니다.");
        language = normalizeLanguage(language);
        final String generationLanguage = language;

        String normalizedCategory = normalizeCategory(category);
        if (user != null) {
            Set<Long> attemptedQuizIds = learningStore.findAttempts(user.getUserId()).stream()
                    .map(com.algopoolja.quiz.learning.QuizAttempt::getQuizId)
                    .collect(java.util.stream.Collectors.toSet());
            var reusable = quizStore.findByCategoryDifficultyAndLanguage(normalizedCategory, difficulty, language)
                    .stream()
                    .filter(quiz -> !attemptedQuizIds.contains(quiz.getQuizId()))
                    .findFirst();
            if (reusable.isPresent()) {
                log.info("Reusing unattempted quiz: quizId={}, category={}, difficulty={}, language={}",
                        reusable.get().getQuizId(), normalizedCategory, difficulty, language);
                return toGenerateResponse(reusable.get());
            }
        }

        ProblemInfo problem;
        BlankQuizInfo blankQuiz;
        if ("one-step".equalsIgnoreCase(generationStrategy)) {
            FullQuizInfo fullQuiz = generator.createFullQuiz(category, difficulty, language);
            problem = fullQuiz.problem();
            blankQuiz = fullQuiz.blankQuiz();
        } else if ("parallel-pipeline".equalsIgnoreCase(generationStrategy)) {
            problem = generateValidatedProblem(category, difficulty,
                    () -> generator.createProblem(category, difficulty, generationLanguage));
            blankQuiz = generateParallelPipeline(problem);
        } else {
            problem = generateValidatedProblem(category, difficulty,
                    () -> generator.createProblem(category, difficulty, generationLanguage));
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
        validateDifficultyMetadata(problem, category, difficulty);
        String blankCode = blankQuiz.getBlankCode();
        AnswerBlockInfo blocks = new AnswerBlockInfo(blankQuiz.getCorrectAnswers(), blankQuiz.getWrongAnswers());
        String validation = tools.validateBlankCount(blankCode, blocks.getCorrectAnswers());
        if (!"OK".equals(validation)) throw new QuizGenerationException(validation);

        List<String> choices = tools.buildChoiceBlocks(blocks.getCorrectAnswers(), blocks.getWrongAnswers());
        List<TestCase> testCases = problem.getTestcases().stream()
                .map(test -> new TestCase(tools.cleanTestcaseText(test.getInput()), tools.cleanTestcaseText(test.getOutput())))
                .toList();

        long id = quizStore.save(problem.getTitle(), problem.getDescription(), normalizedCategory,
                difficulty, language, problem.getCode(), blankCode, blocks.getCorrectAnswers(), choices, testCases,
                problem.getHint(), problem.getExplanation(), problem.getAlgorithm(), problem.getEstimatedLevel(),
                problem.getTimeComplexity(), problem.getSpaceComplexity(), problem.getRequiredTechniques(),
                problem.getCommonMistakes());
        log.info("Quiz generation completed: strategy={}, totalMs={}", generationStrategy, elapsedMillis(startedAt));
        return new GenerateResponse(id, language, problem.getTitle(), problem.getDescription(), testCases, blankCode,
                choices, blocks.getCorrectAnswers().size(), problem.getCode(), problem.getHint(),
                problem.getExplanation(), problem.getAlgorithm(), problem.getEstimatedLevel(),
                problem.getTimeComplexity(), problem.getSpaceComplexity(), problem.getRequiredTechniques(),
                problem.getCommonMistakes());
    }

    private GenerateResponse toGenerateResponse(QuizSnapshot quiz) {
        return new GenerateResponse(quiz.getQuizId(), quiz.getLanguage(), quiz.getTitle(), quiz.getDescription(),
                quiz.getTestcases(), quiz.getBlankCode(), quiz.getAnswerBlocks(), quiz.getCorrectAnswers().size(),
                quiz.getAnswerCode(), quiz.getHint(), quiz.getExplanation(), quiz.getAlgorithm(),
                quiz.getEstimatedLevel(), quiz.getTimeComplexity(), quiz.getSpaceComplexity(),
                quiz.getRequiredTechniques(), quiz.getCommonMistakes());
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
        List<String> correctAnswers;
        List<String> wrongAnswers;
        try {
            correctAnswers = extractCorrectAnswers(problem.getCode(), blankCode);
            wrongAnswers = wrongAnswerGenerator.generate(correctAnswers);
        } catch (QuizGenerationException extractionFailure) {
            // Models can change whitespace while creating blanks. Keep the fast path strict,
            // but fall back to the existing AI answer-block step instead of returning 502.
            log.warn("Local answer extraction failed; falling back to answer-block generation: {}",
                    extractionFailure.getMessage());
            AnswerBlockInfo blocks = generator.createAnswerBlocks(problem.getCode(), blankCode);
            correctAnswers = blocks.getCorrectAnswers();
            wrongAnswers = blocks.getWrongAnswers();
        }
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
            throw new IllegalArgumentException("빈칸 %d개에 모두 답을 입력해주세요.".formatted(quiz.getCorrectAnswers().size()));
        }
        List<String> normalizedAnswers = answers.stream().map(String::strip).toList();
        String completed = quiz.getBlankCode();
        for (String answer : normalizedAnswers) {
            completed = completed.replaceFirst("<BLANK>", java.util.regex.Matcher.quoteReplacement(answer));
        }
        boolean correct = normalizedAnswers.equals(quiz.getCorrectAnswers()) || completed.strip().equals(quiz.getAnswerCode().strip());
        var attempt = learningStore.saveAttempt(user, id, normalizedAnswers, correct, startedAt,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        return new CheckResponse(attempt.getAttemptId(), correct,
                correct ? "정답입니다." : "다시 확인해보세요.", attempt.getAttemptNumber(),
                attempt.getDurationSeconds(), attempt.getAttemptedAt());
    }

    public QuizSnapshot getQuiz(Long id) {
        return quizStore.findById(id).orElseThrow(QuizNotFoundException::new);
    }

    public QuizSnapshot getRandomQuiz(UserIdentity user) {
        List<QuizSnapshot> quizzes = quizStore.findAll();
        if (quizzes.isEmpty()) throw new QuizNotFoundException();
        Set<Long> attemptedQuizIds = user == null ? Set.of() : learningStore.findAttempts(user.getUserId()).stream()
                .map(com.algopoolja.quiz.learning.QuizAttempt::getQuizId)
                .collect(java.util.stream.Collectors.toSet());
        List<QuizSnapshot> unattempted = quizzes.stream()
                .filter(quiz -> !attemptedQuizIds.contains(quiz.getQuizId()))
                .toList();
        List<QuizSnapshot> candidates = unattempted.isEmpty() ? quizzes : unattempted;
        return candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private String normalizeCategory(String value) {
        return "전체".equals(value) ? "ETC" : value.toUpperCase();
    }

    private String normalizeLanguage(String value) {
        String normalized = value == null ? "PYTHON" : value.trim().toUpperCase();
        if (!LANGUAGES.contains(normalized)) throw new IllegalArgumentException("지원하지 않는 언어입니다.");
        return normalized;
    }

    private void requireValidProblem(ProblemInfo problem) {
        if (problem == null || problem.getTitle() == null || problem.getDescription() == null || problem.getCode() == null
                || problem.getTestcases() == null || problem.getTestcases().size() < 2) {
            throw new QuizGenerationException("AI가 유효한 문제 또는 테스트케이스를 생성하지 못했습니다.");
        }
    }

    private void validateDifficultyMetadata(ProblemInfo problem, String category, String difficulty) {
        if (isBlank(problem.getAlgorithm()) || isBlank(problem.getEstimatedLevel())
                || isBlank(problem.getTimeComplexity()) || isBlank(problem.getSpaceComplexity())
                || problem.getRequiredTechniques() == null) {
            throw new QuizGenerationException("AI가 난이도 메타데이터를 완성하지 못했습니다.");
        }
        if (!problem.getEstimatedLevel().strip().toLowerCase(Locale.ROOT).startsWith("프로그래머스")) {
            throw new QuizGenerationException("난이도 메타데이터에는 프로그래머스 레벨을 명시해야 합니다.");
        }
        if (!"전체".equals(category) && !sameAlgorithm(category, problem.getAlgorithm())) {
            throw new QuizGenerationException("요청한 카테고리와 다른 알고리즘이 생성되었습니다.");
        }
        Matcher matcher = LEVEL_PATTERN.matcher(problem.getEstimatedLevel());
        if (!matcher.find()) throw new QuizGenerationException("프로그래머스 레벨 형식이 올바르지 않습니다.");
        int level = Integer.parseInt(matcher.group(1));
        int[] range = switch (difficulty) {
            case "쉬움" -> new int[]{1, 2};
            case "보통" -> new int[]{2, 3};
            case "어려움" -> new int[]{3, 4};
            default -> throw new IllegalArgumentException("지원하지 않는 문제 난이도입니다.");
        };
        if (level < range[0] || level > range[1]) {
            throw new QuizGenerationException("요청한 난이도 범위를 벗어난 문제가 생성되었습니다.");
        }
        if ("어려움".equals(difficulty) && problem.getRequiredTechniques().isEmpty()) {
            throw new QuizGenerationException("어려움 문제에 필요한 알고리즘 기법이 없습니다.");
        }
    }

    private ProblemInfo generateValidatedProblem(String category, String difficulty,
                                                 Supplier<ProblemInfo> generatorCall) {
        QuizGenerationException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            ProblemInfo problem = generatorCall.get();
            try {
                requireValidProblem(problem);
                validateDifficultyMetadata(problem, category, difficulty);
                return problem;
            }
            catch (QuizGenerationException failure) {
                lastFailure = failure;
                log.warn("Rejected generated problem: attempt={}, reason={}", attempt, failure.getMessage());
            }
        }
        throw new QuizGenerationException("요청한 카테고리와 난이도에 맞는 문제를 생성하지 못했습니다. 다시 시도해주세요.", lastFailure);
    }

    private boolean sameAlgorithm(String requested, String generated) {
        return canonicalAlgorithm(requested).equals(canonicalAlgorithm(generated));
    }

    private String canonicalAlgorithm(String value) {
        String normalized = value.strip().toLowerCase(Locale.ROOT).replace(" ", "");
        return switch (normalized) {
            case "동적계획법", "dynamicprogramming" -> "dp";
            case "너비우선탐색", "breadthfirstsearch" -> "bfs";
            case "깊이우선탐색", "depthfirstsearch" -> "dfs";
            case "탐욕법", "greedyalgorithm" -> "greedy";
            default -> normalized;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
