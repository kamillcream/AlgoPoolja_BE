package com.algopoolja.quiz.api;

import com.algopoolja.quiz.api.QuizDtos.ErrorResponse;
import com.algopoolja.quiz.service.QuizGenerationException;
import com.algopoolja.quiz.service.QuizNotFoundException;
import com.algopoolja.quiz.auth.UnauthorizedException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse badRequest(Exception error) { return error("BAD_REQUEST", error.getMessage()); }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorResponse unauthorized(UnauthorizedException error) { return error("UNAUTHORIZED", error.getMessage()); }

    @ExceptionHandler(QuizNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse notFound(QuizNotFoundException error) { return error("QUIZ_NOT_FOUND", error.getMessage()); }

    @ExceptionHandler(QuizGenerationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorResponse generationFailed(QuizGenerationException error) { return error("QUIZ_GENERATION_FAILED", error.getMessage()); }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorResponse internal(Exception error) { return error("INTERNAL_SERVER_ERROR", "요청 처리 중 오류가 발생했습니다."); }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse(code, message, OffsetDateTime.now(SEOUL));
    }
}
