package com.algopoolja.quiz.service;

import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.BlankQuizInfo;
import com.algopoolja.quiz.domain.FullQuizInfo;
import com.algopoolja.quiz.domain.ProblemInfo;

public interface AiQuizGenerator {
    ProblemInfo createProblem(String category, String difficulty);
    String createBlankCode(String answerCode);
    AnswerBlockInfo createAnswerBlocks(String answerCode, String blankCode);

    default BlankQuizInfo createBlankQuiz(String answerCode) {
        String blankCode = createBlankCode(answerCode);
        AnswerBlockInfo blocks = createAnswerBlocks(answerCode, blankCode);
        return new BlankQuizInfo(blankCode, blocks.getCorrectAnswers(), blocks.getWrongAnswers());
    }

    default FullQuizInfo createFullQuiz(String category, String difficulty) {
        ProblemInfo problem = createProblem(category, difficulty);
        BlankQuizInfo blankQuiz = createBlankQuiz(problem.getCode());
        return new FullQuizInfo(problem.getTitle(), problem.getDescription(), problem.getTestcases(),
                problem.getCode(), blankQuiz.getBlankCode(), blankQuiz.getCorrectAnswers(), blankQuiz.getWrongAnswers());
    }
}
