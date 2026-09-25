package com.algopoolja.quiz.persistence;

import com.algopoolja.quiz.domain.QuizSnapshot;
import com.algopoolja.quiz.domain.TestCase;
import java.util.List;
import java.util.Optional;

public interface QuizStore {
    default Long save(String title, String description, String category, String difficulty,
                      String answerCode, String blankCode, List<String> correctAnswers,
                      List<String> answerBlocks, List<TestCase> testCases, String hint,
                      String explanation, String algorithm, String timeComplexity,
                      String spaceComplexity, List<String> commonMistakes) {
        return save(title, description, category, difficulty, "PYTHON", answerCode, blankCode, correctAnswers,
                answerBlocks, testCases, hint, explanation, algorithm, timeComplexity, spaceComplexity,
                commonMistakes);
    }

    default Long save(String title, String description, String category, String difficulty,
              String language, String answerCode, String blankCode, List<String> correctAnswers,
              List<String> answerBlocks, List<TestCase> testCases, String hint,
              String explanation, String algorithm, String timeComplexity,
              String spaceComplexity, List<String> commonMistakes) {
        return save(title, description, category, difficulty, answerCode, blankCode, correctAnswers,
                answerBlocks, testCases, hint, explanation, algorithm, timeComplexity, spaceComplexity,
                commonMistakes);
    }

    Optional<QuizSnapshot> findById(Long quizId);
}
