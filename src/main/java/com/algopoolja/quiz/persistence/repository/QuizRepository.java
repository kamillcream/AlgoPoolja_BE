package com.algopoolja.quiz.persistence.repository;

import com.algopoolja.quiz.persistence.entity.QuizEntity;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {
    Optional<QuizEntity> findByContentHash(String contentHash);

    @EntityGraph(attributePaths = "testCases")
    List<QuizEntity> findByCategoryAndDifficultyAndLanguageOrderByCreatedAtAsc(
            String category, String difficulty, String language);

    @Override
    @EntityGraph(attributePaths = "testCases")
    Optional<QuizEntity> findById(Long id);
}
