package com.algopoolja.quiz.service;

public class QuizNotFoundException extends RuntimeException {
    public QuizNotFoundException() { super("문제를 찾을 수 없습니다. 새 문제를 생성해주세요."); }
}
