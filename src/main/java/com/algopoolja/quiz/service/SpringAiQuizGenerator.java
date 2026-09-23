package com.algopoolja.quiz.service;

import com.algopoolja.quiz.domain.AnswerBlockInfo;
import com.algopoolja.quiz.domain.BlankQuizInfo;
import com.algopoolja.quiz.domain.FullQuizInfo;
import com.algopoolja.quiz.domain.ProblemInfo;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringAiQuizGenerator implements AiQuizGenerator {
    private final ChatClient chatClient;

    @Override
    public ProblemInfo createProblem(String category, String difficulty) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 코딩테스트 문제 생성기입니다.
                        백준이나 프로그래머스 스타일의 새로운 문제를 생성하세요.
                        문제 설명, 테스트케이스 2개 이상, Python 정답 코드를 만드세요.
                        정답 코드는 반드시 하나의 solution 함수로 작성하세요.
                        테스트케이스 입력과 출력은 마크다운이나 라벨 없는 순수 값이어야 합니다.
                        """)
                .user(u -> u.text("""
                        새로운 알고리즘 문제를 하나 생성하세요.
                        문제 유형: {category}
                        난이도: {difficulty}
                        생성 시드: {seed}
                        생성 시간: {now}
                        유형이 '전체'이면 Greedy, DP, BFS, DFS 중 무작위로 선택하세요.
                        """)
                        .param("category", category)
                        .param("difficulty", difficulty)
                        .param("seed", ThreadLocalRandom.current().nextInt(1, 1_000_001))
                        .param("now", LocalDateTime.now()))
                .call()
                .entity(ProblemInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    @Override
    public String createBlankCode(String answerCode) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 문제 빈칸 생성기입니다.
                        원본 줄 수, 들여쓰기, 줄 순서를 유지하세요. 줄 전체를 삭제하면 안 됩니다.
                        핵심 값이나 표현식 1~5개만 정확히 <BLANK>로 바꾸세요.
                        할당문의 왼쪽과 =, return/if/for/while 키워드는 유지하세요.
                        설명, 마크다운, 주석은 추가하지 마세요.
                        """)
                .user("다음 코드에 빈칸을 만드세요.\n\n" + answerCode)
                .call()
                .entity(com.algopoolja.quiz.domain.UserProblem.class,
                        spec -> spec.useProviderStructuredOutput().validateSchema())
                .getProblem();
    }

    @Override
    public AnswerBlockInfo createAnswerBlocks(String answerCode, String blankCode) {
        return chatClient.prompt()
                .system("""
                        원본 코드와 빈칸 코드를 비교해 각 <BLANK>의 정확한 코드 조각을 순서대로 찾으세요.
                        correctAnswers에는 빈칸 수와 같은 개수의 정답만 넣으세요.
                        wrongAnswers에는 비슷하지만 틀린 코드 조각을 3~6개 넣으세요.
                        모든 항목은 설명 없는 코드 조각이어야 합니다.
                        """)
                .user("[원본 정답 코드]\n" + answerCode + "\n\n[빈칸 코드]\n" + blankCode)
                .call()
                .entity(AnswerBlockInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    @Override
    public BlankQuizInfo createBlankQuiz(String answerCode) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 빈칸 학습 콘텐츠 생성기입니다.
                        원본 코드의 줄 수, 들여쓰기, 줄 순서를 유지하세요.
                        핵심 값이나 표현식 1~5개만 정확히 <BLANK>로 바꾸세요.
                        blankCode에는 설명, 마크다운, 주석을 추가하지 마세요.
                        correctAnswers에는 <BLANK>의 정답 코드 조각을 순서대로 넣으세요.
                        wrongAnswers에는 정답과 비슷하지만 틀린 코드 조각을 3~6개 넣으세요.
                        모든 선택지는 설명 없는 코드 조각이어야 합니다.
                        """)
                .user("원본 정답 코드에서 빈칸 코드와 선택지를 함께 생성하세요.\n\n" + answerCode)
                .call()
                .entity(BlankQuizInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    @Override
    public FullQuizInfo createFullQuiz(String category, String difficulty) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 코딩테스트 문제와 빈칸 학습 콘텐츠를 함께 만드는 생성기입니다.
                        문제 설명, 테스트케이스 2개 이상, Python 정답 코드를 생성하세요.
                        정답 코드는 하나의 solution 함수로 작성하세요.
                        blankCode는 원본 코드의 줄 수, 들여쓰기, 줄 순서를 유지하고 핵심 표현식 1~5개만 <BLANK>로 바꾸세요.
                        correctAnswers에는 빈칸 정답을 순서대로, wrongAnswers에는 비슷하지만 틀린 코드 조각을 3~6개 넣으세요.
                        테스트케이스 입력과 출력은 라벨이나 마크다운 없는 순수 값이어야 합니다.
                        설명이나 마크다운은 필드 값에 추가하지 마세요.
                        """)
                .user(u -> u.text("""
                        알고리즘 문제와 빈칸 학습 콘텐츠를 하나 생성하세요.
                        문제 유형: {category}
                        난이도: {difficulty}
                        생성 시드: {seed}
                        생성 시간: {now}
                        """)
                        .param("category", category)
                        .param("difficulty", difficulty)
                        .param("seed", ThreadLocalRandom.current().nextInt(1, 1_000_001))
                        .param("now", LocalDateTime.now()))
                .call()
                .entity(FullQuizInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }
}
