package com.algopoolja.quiz.persistence;

import com.algopoolja.quiz.domain.QuizSnapshot;
import com.algopoolja.quiz.domain.TestCase;
import java.util.List;
import java.util.Optional;

public interface QuizStore {
    Long save(String title, String description, String category, String difficulty,
              String answerCode, String blankCode, List<String> correctAnswers,
              List<String> answerBlocks, List<TestCase> testCases);

    Optional<QuizSnapshot> findById(Long quizId);
}
