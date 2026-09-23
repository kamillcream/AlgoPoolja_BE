package com.algopoolja.quiz.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(nullable = false, length = 50) private String category;
    @Column(nullable = false, length = 20) private String difficulty;
    @Column(name = "answer_code", nullable = false, columnDefinition = "text") private String answerCode;
    @Column(name = "blank_code", nullable = false, columnDefinition = "text") private String blankCode;
    @Column(columnDefinition = "text") private String hint;
    @Column(columnDefinition = "text") private String explanation;
    @Column(length = 100) private String algorithm;
    @Column(name = "time_complexity", length = 100) private String timeComplexity;
    @Column(name = "space_complexity", length = 100) private String spaceComplexity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "common_mistakes", columnDefinition = "jsonb")
    private List<String> commonMistakes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answers", nullable = false, columnDefinition = "jsonb")
    private List<String> correctAnswers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_blocks", nullable = false, columnDefinition = "jsonb")
    private List<String> answerBlocks;

    @Column(name = "content_hash", nullable = false, unique = true, length = 128)
    private String contentHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("caseOrder asc")
    private List<QuizTestCaseEntity> testCases = new ArrayList<>();

    public QuizEntity(String title, String description, String category, String difficulty,
                      String answerCode, String blankCode, List<String> correctAnswers,
                      List<String> answerBlocks, String contentHash, UserEntity createdBy,
                      String hint, String explanation, String algorithm, String timeComplexity,
                      String spaceComplexity, List<String> commonMistakes) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.answerCode = answerCode;
        this.blankCode = blankCode;
        this.correctAnswers = List.copyOf(correctAnswers);
        this.answerBlocks = List.copyOf(answerBlocks);
        this.contentHash = contentHash;
        this.createdBy = createdBy;
        this.hint = hint;
        this.explanation = explanation;
        this.algorithm = algorithm;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.commonMistakes = commonMistakes == null ? List.of() : List.copyOf(commonMistakes);
    }

    public void addTestCase(int order, String input, String expectedOutput) {
        testCases.add(new QuizTestCaseEntity(this, order, input, expectedOutput));
    }
}
