package com.algopoolja.quiz.persistence;

import com.algopoolja.quiz.domain.TestCase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class QuizJdbcRepository {
    private static final String ANONYMOUS_EMAIL = "anonymous@algopoolja.local";

    private final JdbcTemplate jdbc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public QuizJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long saveQuiz(String title, String description, String category, String difficulty,
                         String answerCode, String blankCode, List<String> correctAnswers,
                         List<String> answerBlocks, List<TestCase> testCases) {
        long userId = ensureAnonymousUser();
        String correctJson = toJson(correctAnswers);
        String blocksJson = toJson(answerBlocks);
        String hash = sha256(String.join("|", title, description, category, difficulty, answerCode));
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    insert into quizzes (title, description, category, difficulty, answer_code,
                        blank_code, correct_answers, answer_blocks, content_hash, created_by)
                    values (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                    on conflict (content_hash) do update set content_hash = excluded.content_hash
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setString(3, category);
            statement.setString(4, difficulty);
            statement.setString(5, answerCode);
            statement.setString(6, blankCode);
            statement.setString(7, correctJson);
            statement.setString(8, blocksJson);
            statement.setString(9, hash);
            statement.setLong(10, userId);
            return statement;
        }, keys);
        long quizId = keys.getKey().longValue();
        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            jdbc.update("""
                    insert into quiz_test_cases (quiz_id, case_order, input, expected_output)
                    values (?, ?, ?, ?)
                    on conflict (quiz_id, case_order) do update
                    set input = excluded.input, expected_output = excluded.expected_output
                    """, quizId, i + 1, testCase.input(), testCase.output());
        }
        return quizId;
    }

    public Optional<StoredQuiz> findQuiz(long quizId) {
        List<StoredQuiz> quizzes = jdbc.query("""
                select id, title, description, blank_code, answer_code,
                       correct_answers, answer_blocks
                from quizzes where id = ?
                """, (rs, rowNum) -> new StoredQuiz(
                rs.getLong("id"), rs.getString("title"), rs.getString("description"),
                rs.getString("blank_code"), rs.getString("answer_code"),
                fromJsonArray(rs.getString("correct_answers")),
                fromJsonArray(rs.getString("answer_blocks"))), quizId);
        return quizzes.stream().findFirst();
    }

    public List<TestCase> findTestCases(long quizId) {
        return jdbc.query("""
                select input, expected_output from quiz_test_cases
                where quiz_id = ? order by case_order
                """, (rs, rowNum) -> new TestCase(rs.getString("input"), rs.getString("expected_output")), quizId);
    }

    public void saveAttempt(long quizId, List<String> answers, boolean correct) {
        long userId = ensureAnonymousUser();
        jdbc.update("""
                insert into user_quizzes
                    (user_id, quiz_id, solved, wrong_count, attempt_count, last_attempt_at, next_review_at)
                values (?, ?, ?, ?, 1, now(), case when ? then null else now() end)
                on conflict (user_id, quiz_id) do update set
                    solved = user_quizzes.solved or excluded.solved,
                    wrong_count = user_quizzes.wrong_count + excluded.wrong_count,
                    attempt_count = user_quizzes.attempt_count + 1,
                    last_attempt_at = now(),
                    next_review_at = case when ? then null else now() end
                """, userId, quizId, correct, correct ? 0 : 1, correct, correct);
        jdbc.update("""
                insert into quiz_attempts (user_id, quiz_id, answers, correct)
                values (?, ?, ?::jsonb, ?)
                """, userId, quizId, toJson(answers), correct);
    }

    private long ensureAnonymousUser() {
        Long id = jdbc.queryForObject("""
                insert into users (email, nickname) values (?, 'Anonymous')
                on conflict (email) do update set nickname = excluded.nickname
                returning id
                """, Long.class, ANONYMOUS_EMAIL);
        return id;
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("JSON 변환에 실패했습니다.", exception);
        }
    }

    private List<String> fromJsonArray(String json) {
        try {
            JsonNode node = jsonMapper.readTree(json);
            var values = new java.util.ArrayList<String>();
            node.forEach(item -> values.add(item.asString()));
            return List.copyOf(values);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("JSON 데이터 해석에 실패했습니다.", exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception exception) {
            throw new IllegalStateException("문제 식별자 생성에 실패했습니다.", exception);
        }
    }

    public record StoredQuiz(long id, String title, String description, String blankCode,
                             String answerCode, List<String> correctAnswers, List<String> answerBlocks) {}
}
