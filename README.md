# 코드블록 퀘스트 - Spring AI

LangChain/Gradio로 작성된 알고리즘 빈칸 문제 생성기를 Spring Boot와 Spring AI로 마이그레이션한 프로젝트입니다.

## 실행

요구 환경은 Java 21+입니다. Gradle 8.14.2 Wrapper가 포함되어 있어 Gradle을 별도로 설치할 필요가 없습니다.

```bash
export OPENAI_API_KEY="sk-..."
export OPENAI_MODEL="gpt-4o-mini" # 생략 가능
./gradlew bootRun
```

브라우저에서 `http://localhost:8080`을 엽니다.

테스트와 실행 JAR 생성은 다음 명령을 사용합니다.

```bash
./gradlew test
./gradlew bootJar
```

## API

- `POST /api/quizzes`: `{ "category": "DP", "difficulty": "보통" }`
- `GET /api/quizzes/{quizId}`: 기존 문제 재조회
- `POST /api/quizzes/{quizId}/check`: `{ "answers": ["..."], "startedAt": "2026-09-19T21:38:56+09:00" }`
- `GET /api/dashboard/summary`: 대시보드 통계, 최근 풀이, 복습 문제 통합 조회
- `GET /api/me/quiz-attempts`: 풀이 기록 페이지네이션 및 필터
- `GET /api/me/wrong-quizzes`: 오답·복습 문제 페이지네이션 및 필터

학습 API는 로그인 세션의 `userId`·`nickname` 또는 HS256 Bearer JWT의 `userId`·`nickname` claim으로 사용자를 식별합니다. JWT 사용 시 충분히 긴 `JWT_SECRET` 환경변수를 설정해야 합니다.

생성 결과와 풀이 이력은 현재 서버 메모리에 저장되며 애플리케이션 재시작 시 초기화됩니다. 운영 환경에서는 `LearningStore`와 퀴즈 저장 부분을 영속 저장소 구현으로 교체해야 합니다.

## 원본과의 대응

- Pydantic 구조화 출력 → Lombok DTO + `ChatClient.entity(...)`
- LangChain OpenAI 모델 → Spring AI OpenAI starter
- LangChain Tool → Spring AI `@Tool`
- `create_agent()` → 도구가 기본 등록된 Spring AI `ChatClient`
- Gradio State/UI → 서버 측 퀴즈 상태 + 정적 웹 UI
