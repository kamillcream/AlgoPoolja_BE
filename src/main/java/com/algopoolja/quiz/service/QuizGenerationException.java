package com.algopoolja.quiz.service;

public class QuizGenerationException extends RuntimeException {
    public QuizGenerationException(String message) { super(message); }

    public QuizGenerationException(String message, Throwable cause) { super(message, cause); }
}
