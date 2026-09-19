package com.algopoolja.quiz.learning;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class LearningStore {
    private final AtomicLong sequence = new AtomicLong();
    private final List<QuizAttempt> attempts = new ArrayList<>();
    private final Map<Long, UserIdentity> users = new ConcurrentHashMap<>();

    public synchronized QuizAttempt saveAttempt(UserIdentity user, Long quizId, List<String> answers,
                                                boolean correct, OffsetDateTime startedAt,
                                                OffsetDateTime attemptedAt) {
        users.put(user.getUserId(), user);
        int attemptNumber = (int) attempts.stream()
                .filter(attempt -> attempt.getUserId().equals(user.getUserId()))
                .filter(attempt -> attempt.getQuizId().equals(quizId))
                .count() + 1;
        QuizAttempt attempt = new QuizAttempt(sequence.incrementAndGet(), user.getUserId(), quizId,
                List.copyOf(answers), correct, attemptNumber, startedAt, attemptedAt);
        attempts.add(attempt);
        return attempt;
    }

    public synchronized List<QuizAttempt> findAttempts(Long userId) {
        return attempts.stream()
                .filter(attempt -> attempt.getUserId().equals(userId))
                .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt).reversed())
                .toList();
    }

    public UserIdentity findUser(Long userId) {
        return users.get(userId);
    }
}
