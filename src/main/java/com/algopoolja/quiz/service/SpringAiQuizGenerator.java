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
        return createProblem(category, difficulty, "PYTHON");
    }

    @Override
    public ProblemInfo createProblem(String category, String difficulty, String language) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 코딩테스트 문제 생성기입니다.
                        프로그래머스 레벨 2~3 및 국내 기업 코딩테스트의 출제 의도를 참고한
                        새로운 문제를 생성하세요. 공개된 기존 문제를 복사하거나 이름만 바꾸지 마세요.
                        단순한 문법·반복문 문제는 피하고, 자료구조 선택과 알고리즘 설계가 필요한 문제를 만드세요.
                        문제 설명에는 입력 제한, 출력 형식, 시간 복잡도에 영향을 주는 조건을 포함하세요.
                        테스트케이스는 예제뿐 아니라 중복값, 최소·최대 경계, 불가능한 경우를 포함해 3개 이상 만드세요.
                        정답 코드의 모든 분기와 예외 처리 조건을 문제 설명에 명시하세요.
                        예를 들어 코드가 n < 2, 빈 입력, 해답 없음 등을 처리한다면 해당 조건과 반환값을 반드시 설명에 포함하고,
                        그 조건을 검증하는 테스트케이스를 최소 1개 포함하세요. 설명에 없는 숨은 예외 처리를 정답 코드에 넣지 마세요.
                        문제 설명, 테스트케이스, 요청한 언어의 정답 코드를 만드세요.
                        hint에는 정답 코드를 직접 노출하지 않는 단계별 힌트를 1~2문장으로 작성하세요.
                        explanation에는 알고리즘 선택 이유와 풀이 과정을 작성하세요.
                        algorithm에는 대표 알고리즘 또는 자료구조 이름을 작성하세요.
                        estimatedLevel에는 반드시 '프로그래머스 Lv.1'부터 '프로그래머스 Lv.4' 중 하나를 작성하세요.
                        timeComplexity와 spaceComplexity에는 Big-O 표기법을 작성하세요.
                        requiredTechniques에는 문제 풀이에 실제로 필요한 알고리즘 기법을 작성하세요.
                        commonMistakes에는 학습자가 자주 하는 실수를 2~4개 작성하세요.
                        정답 코드는 언어별 규칙에 맞는 하나의 solution 진입점으로 작성하세요.
                        테스트케이스 입력과 출력은 마크다운이나 라벨 없는 순수 값이어야 합니다.
                        """)
                .user(u -> u.text("""
                        새로운 알고리즘 문제를 하나 생성하세요.
                        문제 유형: {category}
                        난이도: {difficulty}
                        구현 언어: {language}
                        언어별 구현 규칙: {languageGuide}
                        유형별 출제 방향: {categoryGuide}
                        난이도별 요구사항: {difficultyGuide}
                        카테고리를 난이도보다 우선하세요. 요청한 유형이 '전체'가 아니면 algorithm은 반드시 요청 유형과 일치해야 합니다.
                        생성 시드: {seed}
                        생성 시간: {now}
                        유형이 '전체'이면 아래 유형 중 하나를 무작위로 선택하세요:
                        구현/시뮬레이션, 스택/큐, 해시/정렬, 이분 탐색, 힙/우선순위 큐,
                        완전탐색/백트래킹, 그래프, 문자열/파싱, Greedy, DP, BFS, DFS.
                        """)
                        .param("category", category)
                        .param("difficulty", difficulty)
                        .param("language", language)
                        .param("languageGuide", languageGuide(language))
                        .param("categoryGuide", categoryGuide(category))
                        .param("difficultyGuide", difficultyGuide(difficulty))
                        .param("seed", ThreadLocalRandom.current().nextInt(1, 1_000_001))
                        .param("now", LocalDateTime.now()))
                .call()
                .entity(ProblemInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }

    private String languageGuide(String language) {
        return switch (language) {
            case "JAVASCRIPT" -> "Node.js 20 기준. 하나의 solution 함수로 작성하고 마지막에 호출하지 마세요. 표준 입력/출력 코드는 사용하지 마세요.";
            case "JAVA" -> "Java 17 기준. public class Solution 안에 public static 메서드 solution을 작성하세요. 패키지와 main 메서드는 넣지 마세요.";
            case "CPP" -> "C++17 기준. class Solution 안에 public 메서드 solution을 작성하세요. main 함수와 입출력 코드는 넣지 마세요.";
            default -> "Python 3 기준. 하나의 solution 함수로 작성하고 표준 입력/출력 코드는 사용하지 마세요.";
        };
    }

    private String categoryGuide(String category) {
        return switch (category) {
            case "구현/시뮬레이션" -> "상태 변화, 순서 의존성, 다단계 조건을 정확히 처리하세요.";
            case "스택/큐" -> "LIFO/FIFO 또는 단조 스택·큐가 필요한 상황을 설계하세요.";
            case "해시/정렬" -> "정렬 기준과 빈도·그룹화·중복 처리를 결합하세요.";
            case "이분 탐색" -> "가능 여부 판정 함수와 탐색 범위를 설계해야 하도록 하세요.";
            case "힙/우선순위 큐" -> "현재 최솟값·최댓값을 반복적으로 선택하는 과정을 포함하세요.";
            case "완전탐색/백트래킹" -> "탐색 공간을 가지치기하거나 중복 상태를 제거해야 하도록 하세요.";
            case "그래프" -> "간선, 연결 요소, 최단 경로 또는 위상 관계 중 하나를 활용하세요.";
            case "문자열/파싱" -> "토큰화·구간·괄호·날짜 등 입력 해석과 자료구조 처리를 결합하세요.";
            case "Greedy" -> "선택 기준의 타당성을 설명할 수 있고 반례를 고려하게 하세요.";
            case "DP" -> "상태 정의와 전이식이 필요하며 단순 누적 문제가 아니도록 하세요.";
            case "BFS", "DFS" -> "방문 처리와 상태 또는 경로 복원이 필요한 그래프 문제로 구성하세요.";
            default -> "서로 다른 알고리즘 전략을 비교해야 하는 중급 문제로 구성하세요.";
        };
    }

    private String difficultyGuide(String difficulty) {
        return switch (difficulty) {
            case "쉬움" -> "프로그래머스 Lv.1~2. 핵심 알고리즘 1개를 사용하되 경계 조건과 구현 실수를 유도하세요.";
            case "보통" -> "프로그래머스 Lv.2~3. 핵심 알고리즘 1개와 자료구조 1개를 결합하고, N이 100,000 이상인 제한을 고려하세요.";
            case "어려움" -> "프로그래머스 Lv.3~4. 두 가지 이상의 개념을 결합하고, 단순 O(N^2) 풀이가 시간 초과되도록 제한을 설정하세요.";
            default -> "레벨 2~3 수준의 시간·공간 복잡도 판단이 필요한 문제로 구성하세요.";
        };
    }

    @Override
    public String createBlankCode(String answerCode) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 문제 빈칸 생성기입니다.
                        원본 줄 수, 들여쓰기, 줄 순서를 유지하세요. 줄 전체를 삭제하면 안 됩니다.
                        핵심 값이나 표현식을 3개 이상 정확히 <BLANK>로 바꾸세요.
                        빈칸은 반드시 알고리즘의 결과를 바꾸는 핵심 로직에만 만드세요.
                        함수명, 함수 시그니처, 매개변수명, 이미 선언된 변수명, import,
                        자료형 표기와 형식 요소는 절대 빈칸으로 만들지 마세요.
                        반복 범위, 종료 조건, 상태 전이식, 비교식, 자료구조 연산처럼
                        학습자가 알고리즘을 이해해야 결정할 수 있는 표현식을 우선 선택하세요.
                        단순 식별자 맞히기 문제가 되지 않도록 각 빈칸이 코드 동작과 직접 관련되게 하세요.
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
                        핵심 값이나 표현식을 3개 이상 정확히 <BLANK>로 바꾸세요.
                        함수명, 함수 시그니처, 매개변수명, 이미 선언된 변수명, import,
                        자료형 표기와 형식 요소는 절대 빈칸으로 만들지 마세요.
                        반복 범위, 종료 조건, 상태 전이식, 비교식, 자료구조 연산 등
                        알고리즘의 결과를 결정하는 핵심 로직만 빈칸으로 만드세요.
                        단순 식별자 맞히기 문제가 되지 않도록 각 빈칸이 코드 동작과 직접 관련되게 하세요.
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
        return createFullQuiz(category, difficulty, "PYTHON");
    }

    @Override
    public FullQuizInfo createFullQuiz(String category, String difficulty, String language) {
        return chatClient.prompt()
                .system("""
                        당신은 알고리즘 코딩테스트 문제와 빈칸 학습 콘텐츠를 함께 만드는 생성기입니다.
                        문제 설명, 테스트케이스 2개 이상, 요청한 언어의 정답 코드를 생성하세요.
                        정답 코드의 모든 분기와 예외 처리 조건을 문제 설명에 명시하고,
                        각 예외 조건을 검증하는 테스트케이스를 최소 1개 포함하세요. 설명에 없는 숨은 예외 처리는 넣지 마세요.
                        정답 코드는 언어별 규칙에 맞는 하나의 solution 진입점으로 작성하세요.
                        blankCode는 원본 코드의 줄 수, 들여쓰기, 줄 순서를 유지하고 핵심 표현식을 3개 이상 <BLANK>로 바꾸세요.
                        함수명, 함수 시그니처, 매개변수명, 이미 선언된 변수명, import와 형식 요소는
                        절대 빈칸으로 만들지 말고, 반복 범위·종료 조건·상태 전이식·비교식·자료구조 연산 등
                        알고리즘의 결과를 결정하는 핵심 로직만 빈칸으로 만드세요.
                        correctAnswers에는 빈칸 정답을 순서대로, wrongAnswers에는 비슷하지만 틀린 코드 조각을 3~6개 넣으세요.
                        테스트케이스 입력과 출력은 라벨이나 마크다운 없는 순수 값이어야 합니다.
                        설명이나 마크다운은 필드 값에 추가하지 마세요.
                        hint, explanation, algorithm, estimatedLevel(프로그래머스 Lv.1~4),
                        timeComplexity, spaceComplexity, requiredTechniques, commonMistakes
                        필드도 문제와 함께 생성하세요.
                        """)
                .user(u -> u.text("""
                        알고리즘 문제와 빈칸 학습 콘텐츠를 하나 생성하세요.
                        문제 유형: {category}
                        난이도: {difficulty}
                        구현 언어: {language}
                        카테고리를 난이도보다 우선하고, 요청 유형이 전체가 아니면 algorithm을 요청 유형과 일치시키세요.
                        estimatedLevel은 프로그래머스 Lv.1~4 중 난이도에 맞게 선택하세요.
                        언어별 구현 규칙: {languageGuide}
                        생성 시드: {seed}
                        생성 시간: {now}
                        """)
                        .param("category", category)
                        .param("difficulty", difficulty)
                        .param("language", language)
                        .param("languageGuide", languageGuide(language))
                        .param("seed", ThreadLocalRandom.current().nextInt(1, 1_000_001))
                        .param("now", LocalDateTime.now()))
                .call()
                .entity(FullQuizInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
    }
}
