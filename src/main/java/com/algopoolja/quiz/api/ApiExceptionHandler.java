package com.algopoolja.quiz.api;

import com.algopoolja.quiz.api.QuizDtos.ErrorResponse;
import com.algopoolja.quiz.service.QuizGenerationException;
import com.algopoolja.quiz.service.QuizNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse badRequest(Exception error) { return new ErrorResponse(error.getMessage()); }

    @ExceptionHandler(QuizNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse notFound(QuizNotFoundException error) { return new ErrorResponse(error.getMessage()); }

    @ExceptionHandler(QuizGenerationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorResponse generationFailed(QuizGenerationException error) { return new ErrorResponse(error.getMessage()); }
}
