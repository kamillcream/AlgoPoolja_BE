package com.algopoolja.quiz.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_quizzes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quiz_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserQuizEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quiz_id") private QuizEntity quiz;
    @Column(nullable = false) private boolean solved;
    @Column(name = "wrong_count", nullable = false) private int wrongCount;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "last_attempt_at") private OffsetDateTime lastAttemptAt;
    @Column(name = "next_review_at") private OffsetDateTime nextReviewAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    public UserQuizEntity(UserEntity user, QuizEntity quiz) {
        this.user = user;
        this.quiz = quiz;
    }

    public void recordAttempt(boolean correct, OffsetDateTime attemptedAt) {
        attemptCount++;
        if (correct) {
            solved = true;
            nextReviewAt = null;
        } else {
            wrongCount++;
            nextReviewAt = attemptedAt.plusDays(1);
        }
        lastAttemptAt = attemptedAt;
    }
}
