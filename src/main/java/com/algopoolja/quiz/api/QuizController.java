package com.algopoolja.quiz.api;

import com.algopoolja.quiz.api.QuizDtos.CheckRequest;
import com.algopoolja.quiz.api.QuizDtos.CheckResponse;
import com.algopoolja.quiz.api.QuizDtos.GenerateRequest;
import com.algopoolja.quiz.api.QuizDtos.GenerateResponse;
import com.algopoolja.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService service;

    public QuizController(QuizService service) { this.service = service; }

    @PostMapping
    public GenerateResponse generate(@Valid @RequestBody GenerateRequest request) {
        return service.generate(request.category(), request.difficulty());
    }

    @PostMapping("/{quizId}/check")
    public CheckResponse check(@PathVariable Long quizId, @Valid @RequestBody CheckRequest request) {
        return service.check(quizId, request.answers());
    }
}
