package com.algopoolja.quiz.learning;

import java.time.OffsetDateTime;
import java.util.List;

public interface LearningStore {
    QuizAttempt saveAttempt(UserIdentity user, Long quizId, List<String> answers, boolean correct,
                            OffsetDateTime startedAt, OffsetDateTime attemptedAt);

    List<QuizAttempt> findAttempts(Long userId);

    UserIdentity findUser(Long userId);
}
