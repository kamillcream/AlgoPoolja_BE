package com.algopoolja.quiz.persistence.repository;

import com.algopoolja.quiz.persistence.entity.UserQuizEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizRepository extends JpaRepository<UserQuizEntity, Long> {
    Optional<UserQuizEntity> findByUserIdAndQuizId(Long userId, Long quizId);
}
