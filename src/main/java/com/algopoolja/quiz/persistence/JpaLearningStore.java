package com.algopoolja.quiz.persistence;

import com.algopoolja.quiz.learning.*;
import com.algopoolja.quiz.persistence.entity.*;
import com.algopoolja.quiz.persistence.repository.*;
import com.algopoolja.quiz.service.QuizNotFoundException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaLearningStore implements LearningStore {
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final QuizAttemptRepository attemptRepository;

    @Override
    @Transactional
    public QuizAttempt saveAttempt(UserIdentity identity, Long quizId, List<String> answers,
                                   boolean correct, OffsetDateTime startedAt,
                                   OffsetDateTime attemptedAt) {
        UserEntity user = userRepository.findById(identity.getUserId())
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
        QuizEntity quiz = quizRepository.findById(quizId).orElseThrow(QuizNotFoundException::new);
        UserQuizEntity userQuiz = userQuizRepository.findByUserIdAndQuizId(user.getId(), quizId)
                .orElseGet(() -> new UserQuizEntity(user, quiz));
        userQuiz.recordAttempt(correct, attemptedAt);
        userQuizRepository.save(userQuiz);

        Long duration = startedAt == null ? null
                : Math.max(0L, Duration.between(startedAt, attemptedAt).getSeconds());
        QuizAttemptEntity saved = attemptRepository.save(new QuizAttemptEntity(user, quiz, answers, correct,
                userQuiz.getAttemptCount(), startedAt, duration, attemptedAt));
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> findAttempts(Long userId) {
        return attemptRepository.findAllByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> findAttemptsBetween(OffsetDateTime from, OffsetDateTime to) {
        return attemptRepository
                .findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThanOrderBySubmittedAtDesc(from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserIdentity findUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserIdentity(user.getId(), user.getNickname()))
                .orElse(null);
    }

    private QuizAttempt toDomain(QuizAttemptEntity entity) {
        return new QuizAttempt(entity.getId(), entity.getUser().getId(), entity.getQuiz().getId(),
                List.copyOf(entity.getAnswers()), entity.isCorrect(), entity.getAttemptNumber(),
                entity.getStartedAt(), entity.getSubmittedAt());
    }
}
