package com.algopoolja.quiz.service;

import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.ProblemInfo;

public interface AiQuizGenerator {
    ProblemInfo createProblem(String category, String difficulty);
    String createBlankCode(String answerCode);
    AnswerBlockInfo createAnswerBlocks(String answerCode, String blankCode);
}
