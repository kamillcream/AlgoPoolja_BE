package com.algopoolja.quiz.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "quiz_test_cases", uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "case_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizTestCaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;
    @Column(name = "case_order", nullable = false) private int caseOrder;
    @Column(nullable = false, columnDefinition = "text") private String input;
    @Column(name = "expected_output", nullable = false, columnDefinition = "text") private String expectedOutput;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    QuizTestCaseEntity(QuizEntity quiz, int caseOrder, String input, String expectedOutput) {
        this.quiz = quiz;
        this.caseOrder = caseOrder;
        this.input = input;
        this.expectedOutput = expectedOutput;
    }
}
