package com.algopoolja.quiz.support;

import com.algopoolja.quiz.domain.QuizSnapshot;
import com.algopoolja.quiz.domain.TestCase;
import com.algopoolja.quiz.persistence.QuizStore;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryQuizStore implements QuizStore {
    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, QuizSnapshot> quizzes = new HashMap<>();

    @Override
    public Long save(String title, String description, String category, String difficulty,
                     String answerCode, String blankCode, List<String> correctAnswers,
                     List<String> answerBlocks, List<TestCase> testCases, String hint,
                     String explanation, String algorithm, String timeComplexity,
                     String spaceComplexity, List<String> commonMistakes) {
        long id = sequence.incrementAndGet();
        String apiDifficulty = switch (difficulty) {
            case "쉬움" -> "EASY"; case "보통" -> "NORMAL"; case "어려움" -> "HARD"; default -> difficulty;
        };
        quizzes.put(id, new QuizSnapshot(id, title, description, category, apiDifficulty,
                List.copyOf(testCases), answerCode, blankCode, List.copyOf(correctAnswers), List.copyOf(answerBlocks),
                hint, explanation, algorithm, timeComplexity, spaceComplexity,
                commonMistakes == null ? List.of() : List.copyOf(commonMistakes)));
        return id;
    }

    @Override
    public Optional<QuizSnapshot> findById(Long quizId) { return Optional.ofNullable(quizzes.get(quizId)); }

    @Override
    public List<QuizSnapshot> findAll() { return List.copyOf(quizzes.values()); }

    @Override
    public List<QuizSnapshot> findByCategoryDifficultyAndLanguage(String category, String difficulty,
                                                                    String language) {
        return quizzes.values().stream()
                .filter(quiz -> quiz.getCategory().equals(category)
                        && quiz.getDifficulty().equals(switch (difficulty) {
                            case "쉬움" -> "EASY";
                            case "보통" -> "NORMAL";
                            case "어려움" -> "HARD";
                            default -> difficulty;
                        })
                        && quiz.getLanguage().equals(language))
                .toList();
    }
}
