package com.algopoolja.quiz.service;

import com.algopoolja.quiz.api.QuizDtos.CheckResponse;
import com.algopoolja.quiz.api.QuizDtos.GenerateResponse;
import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.ProblemInfo;
import com.algopoolja.quiz.domain.TestCase;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class QuizService {
    private static final Set<String> CATEGORIES = Set.of("전체", "Greedy", "DP", "BFS", "DFS");
    private static final Set<String> DIFFICULTIES = Set.of("쉬움", "보통", "어려움");

    private final AiQuizGenerator generator;
    private final QuizTools tools;
    private final ConcurrentHashMap<UUID, StoredQuiz> quizzes = new ConcurrentHashMap<>();

    public QuizService(AiQuizGenerator generator, QuizTools tools) {
        this.generator = generator;
        this.tools = tools;
    }

    public GenerateResponse generate(String category, String difficulty) {
        if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("지원하지 않는 문제 유형입니다.");
        if (!DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException("지원하지 않는 난이도입니다.");

        ProblemInfo problem = generator.createProblem(category, difficulty);
        requireValidProblem(problem);
        String blankCode = generator.createBlankCode(problem.code());
        AnswerBlockInfo blocks = generator.createAnswerBlocks(problem.code(), blankCode);
        String validation = tools.validateBlankCount(blankCode, blocks.correctAnswers());
        if (!"OK".equals(validation)) throw new QuizGenerationException(validation);

        List<String> choices = tools.buildChoiceBlocks(blocks.correctAnswers(), blocks.wrongAnswers());
        List<TestCase> testCases = problem.testcases().stream()
                .map(test -> new TestCase(tools.cleanTestcaseText(test.input()), tools.cleanTestcaseText(test.output())))
                .toList();
        UUID id = UUID.randomUUID();
        quizzes.put(id, new StoredQuiz(problem.code(), blankCode, List.copyOf(blocks.correctAnswers())));
        return new GenerateResponse(id, problem.title(), problem.description(), testCases, blankCode,
                choices, blocks.correctAnswers().size(), problem.code());
    }

    public CheckResponse check(UUID id, List<String> answers) {
        StoredQuiz quiz = quizzes.get(id);
        if (quiz == null) throw new QuizNotFoundException();
        if (answers.size() != quiz.correctAnswers().size()) {
            return new CheckResponse(false, "빈칸 %d개를 모두 선택해주세요.".formatted(quiz.correctAnswers().size()), null);
        }
        String completed = quiz.blankCode();
        for (String answer : answers) completed = completed.replaceFirst("<BLANK>", java.util.regex.Matcher.quoteReplacement(answer));
        boolean correct = answers.equals(quiz.correctAnswers()) || completed.strip().equals(quiz.answerCode().strip());
        return new CheckResponse(correct,
                correct ? "정답입니다. 선택한 블록이 원본 코드와 일치합니다." : "다시 확인해보세요. 원본 코드와 일치하지 않습니다.",
                completed);
    }

    private void requireValidProblem(ProblemInfo problem) {
        if (problem == null || problem.title() == null || problem.description() == null || problem.code() == null
                || problem.testcases() == null || problem.testcases().size() < 2) {
            throw new QuizGenerationException("AI가 유효한 문제 또는 테스트케이스를 생성하지 못했습니다.");
        }
    }

    private record StoredQuiz(String answerCode, String blankCode, List<String> correctAnswers) {}
}
