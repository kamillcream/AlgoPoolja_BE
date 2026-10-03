package com.algopoolja.quiz.support;

import com.algopoolja.quiz.learning.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryLearningStore implements LearningStore {
    private final AtomicLong sequence = new AtomicLong();
    private final List<QuizAttempt> attempts = new ArrayList<>();
    private final Map<Long, UserIdentity> users = new HashMap<>();

    @Override
    public QuizAttempt saveAttempt(UserIdentity user, Long quizId, List<String> answers, boolean correct,
                                   OffsetDateTime startedAt, OffsetDateTime attemptedAt) {
        users.put(user.getUserId(), user);
        int number = (int) attempts.stream().filter(a -> a.getUserId().equals(user.getUserId()))
                .filter(a -> a.getQuizId().equals(quizId)).count() + 1;
        QuizAttempt attempt = new QuizAttempt(sequence.incrementAndGet(), user.getUserId(), quizId,
                List.copyOf(answers), correct, number, startedAt, attemptedAt);
        attempts.add(attempt);
        return attempt;
    }

    @Override
    public List<QuizAttempt> findAttempts(Long userId) {
        return attempts.stream().filter(a -> a.getUserId().equals(userId))
                .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt).reversed()).toList();
    }

    @Override
    public List<QuizAttempt> findAttemptsBetween(OffsetDateTime from, OffsetDateTime to) {
        return attempts.stream()
                .filter(a -> !a.getAttemptedAt().isBefore(from) && a.getAttemptedAt().isBefore(to))
                .sorted(Comparator.comparing(QuizAttempt::getAttemptedAt).reversed()).toList();
    }

    @Override
    public UserIdentity findUser(Long userId) { return users.get(userId); }
}
