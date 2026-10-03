package com.algopoolja.quiz.persistence.repository;

import com.algopoolja.quiz.persistence.entity.QuizAttemptEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    long countByUserIdAndQuizId(Long userId, Long quizId);

    @EntityGraph(attributePaths = {"user", "quiz"})
    List<QuizAttemptEntity> findAllByUserIdOrderBySubmittedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "quiz"})
    List<QuizAttemptEntity> findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThanOrderBySubmittedAtDesc(
            java.time.OffsetDateTime from, java.time.OffsetDateTime to);
}
