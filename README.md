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
- `POST /api/quizzes/{quizId}/check`: `{ "answers": ["..."] }`

생성 결과는 현재 서버 메모리에 저장됩니다. 운영 환경에서 다중 인스턴스로 실행하려면 `QuizService`의 저장소를 Redis나 DB 구현으로 교체하세요.

## 원본과의 대응

- Pydantic 구조화 출력 → Java record + `ChatClient.entity(...)`
- LangChain OpenAI 모델 → Spring AI OpenAI starter
- LangChain Tool → Spring AI `@Tool`
- `create_agent()` → 도구가 기본 등록된 Spring AI `ChatClient`
- Gradio State/UI → 서버 측 퀴즈 상태 + 정적 웹 UI
