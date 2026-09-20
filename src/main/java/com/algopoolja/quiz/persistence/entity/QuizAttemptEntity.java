package com.algopoolja.quiz.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quiz_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quiz_id") private QuizEntity quiz;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> answers;
    @Column(nullable = false) private boolean correct;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(name = "started_at") private OffsetDateTime startedAt;
    @Column(name = "duration_seconds") private Long durationSeconds;
    @Column(name = "submitted_at", nullable = false) private OffsetDateTime submittedAt;

    public QuizAttemptEntity(UserEntity user, QuizEntity quiz, List<String> answers, boolean correct,
                             int attemptNumber, OffsetDateTime startedAt, Long durationSeconds,
                             OffsetDateTime submittedAt) {
        this.user = user;
        this.quiz = quiz;
        this.answers = List.copyOf(answers);
        this.correct = correct;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.submittedAt = submittedAt;
    }
}
