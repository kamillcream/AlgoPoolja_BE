package com.algopoolja.quiz.api;

import com.algopoolja.quiz.api.QuizDtos.CheckRequest;
import com.algopoolja.quiz.api.QuizDtos.CheckResponse;
import com.algopoolja.quiz.api.QuizDtos.GenerateRequest;
import com.algopoolja.quiz.api.QuizDtos.GenerateResponse;
import com.algopoolja.quiz.api.QuizDtos.QuizDetailResponse;
import com.algopoolja.quiz.service.QuizService;
import com.algopoolja.quiz.auth.CurrentUserProvider;
import com.algopoolja.quiz.domain.QuizSnapshot;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService service;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public GenerateResponse generate(@Valid @RequestBody GenerateRequest request,
                                     HttpServletRequest servletRequest) {
        return service.generate(request.getCategory(), request.getDifficulty(), request.getLanguage(),
                currentUserProvider.requireCurrentUser(servletRequest));
    }

    @PostMapping("/{quizId}/check")
    public CheckResponse check(@PathVariable Long quizId, @Valid @RequestBody CheckRequest request,
                               HttpServletRequest servletRequest) {
        return service.check(quizId, request.getAnswers(), request.getStartedAt(),
                currentUserProvider.requireCurrentUser(servletRequest));
    }

    @GetMapping("/{quizId}")
    public QuizDetailResponse getQuiz(@PathVariable Long quizId) {
        QuizSnapshot quiz = service.getQuiz(quizId);
        return new QuizDetailResponse(quiz.getQuizId(), quiz.getLanguage(), quiz.getTitle(), quiz.getDescription(), quiz.getCategory(),
                quiz.getDifficulty(), quiz.getTestcases(), quiz.getBlankCode(), quiz.getAnswerBlocks(),
                quiz.getCorrectAnswers().size(), quiz.getAnswerCode(), quiz.getHint(), quiz.getExplanation(), quiz.getAlgorithm(),
                quiz.getTimeComplexity(), quiz.getSpaceComplexity(), quiz.getCommonMistakes());
    }
}
