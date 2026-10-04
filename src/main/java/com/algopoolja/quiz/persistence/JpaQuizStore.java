package com.algopoolja.quiz.persistence;

import com.algopoolja.quiz.domain.QuizSnapshot;
import com.algopoolja.quiz.domain.TestCase;
import com.algopoolja.quiz.persistence.entity.QuizEntity;
import com.algopoolja.quiz.persistence.repository.QuizRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaQuizStore implements QuizStore {
    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public Long save(String title, String description, String category, String difficulty,
                     String answerCode, String blankCode, List<String> correctAnswers,
                     List<String> answerBlocks, List<TestCase> testCases, String hint,
                     String explanation, String algorithm, String timeComplexity,
                     String spaceComplexity, List<String> commonMistakes) {
        return save(title, description, category, difficulty, "PYTHON", answerCode, blankCode, correctAnswers,
                answerBlocks, testCases, hint, explanation, algorithm, timeComplexity, spaceComplexity,
                commonMistakes);
    }

    @Override
    @Transactional
    public Long save(String title, String description, String category, String difficulty, String language,
                     String answerCode, String blankCode, List<String> correctAnswers,
                     List<String> answerBlocks, List<TestCase> testCases, String hint,
                     String explanation, String algorithm, String timeComplexity,
                     String spaceComplexity, List<String> commonMistakes) {
        String hash = sha256(String.join("|", title, description, category, difficulty, language, answerCode));
        Optional<QuizEntity> existing = quizRepository.findByContentHash(hash);
        if (existing.isPresent()) return existing.get().getId();

        QuizEntity quiz = new QuizEntity(title, description, category, difficulty, language, answerCode,
                blankCode, correctAnswers, answerBlocks, hash, null, hint, explanation,
                algorithm, timeComplexity, spaceComplexity, commonMistakes);
        for (int index = 0; index < testCases.size(); index++) {
            TestCase test = testCases.get(index);
            quiz.addTestCase(index + 1, test.getInput(), test.getOutput());
        }
        return quizRepository.save(quiz).getId();
    }

    @Override
    @Transactional
    public Long save(String title, String description, String category, String difficulty, String language,
                     String answerCode, String blankCode, List<String> correctAnswers,
                     List<String> answerBlocks, List<TestCase> testCases, String hint,
                     String explanation, String algorithm, String estimatedLevel,
                     String timeComplexity, String spaceComplexity, List<String> requiredTechniques,
                     List<String> commonMistakes) {
        String hash = sha256(String.join("|", title, description, category, difficulty, language, answerCode));
        Optional<QuizEntity> existing = quizRepository.findByContentHash(hash);
        if (existing.isPresent()) return existing.get().getId();

        QuizEntity quiz = new QuizEntity(title, description, category, difficulty, language, answerCode,
                blankCode, correctAnswers, answerBlocks, hash, null, hint, explanation,
                algorithm, estimatedLevel, timeComplexity, spaceComplexity, requiredTechniques, commonMistakes);
        for (int index = 0; index < testCases.size(); index++) {
            TestCase test = testCases.get(index);
            quiz.addTestCase(index + 1, test.getInput(), test.getOutput());
        }
        return quizRepository.save(quiz).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizSnapshot> findById(Long quizId) {
        return quizRepository.findById(quizId).map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSnapshot> findAll() {
        return quizRepository.findAllByOrderByCreatedAtAsc().stream().map(this::toSnapshot).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSnapshot> findByCategoryDifficultyAndLanguage(String category, String difficulty,
                                                                   String language) {
        return quizRepository.findByCategoryAndDifficultyAndLanguageOrderByCreatedAtAsc(category, difficulty, language)
                .stream().map(this::toSnapshot).toList();
    }

    private QuizSnapshot toSnapshot(QuizEntity quiz) {
        List<TestCase> tests = quiz.getTestCases().stream()
                .map(test -> new TestCase(test.getInput(), test.getExpectedOutput()))
                .toList();
        return new QuizSnapshot(quiz.getId(), quiz.getLanguage() == null ? "PYTHON" : quiz.getLanguage(),
                quiz.getTitle(), quiz.getDescription(), quiz.getCategory(),
                apiDifficulty(quiz.getDifficulty()), tests, quiz.getAnswerCode(), quiz.getBlankCode(),
                List.copyOf(quiz.getCorrectAnswers()), List.copyOf(quiz.getAnswerBlocks()), quiz.getHint(),
                quiz.getExplanation(), quiz.getAlgorithm(), quiz.getEstimatedLevel(), quiz.getTimeComplexity(), quiz.getSpaceComplexity(),
                quiz.getRequiredTechniques() == null ? List.of() : List.copyOf(quiz.getRequiredTechniques()),
                quiz.getCommonMistakes() == null ? List.of() : List.copyOf(quiz.getCommonMistakes()));
    }

    private String apiDifficulty(String difficulty) {
        return switch (difficulty) {
            case "쉬움" -> "EASY";
            case "보통" -> "NORMAL";
            case "어려움" -> "HARD";
            default -> difficulty;
        };
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("문제 식별자 생성에 실패했습니다.", exception);
        }
    }
}
