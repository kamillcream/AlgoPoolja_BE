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

### Google OAuth

Google Cloud Console에서 OAuth 2.0 웹 클라이언트를 만들고 승인된 리디렉션 URI로 다음 주소를 등록합니다.

```text
http://localhost:8080/login/oauth2/code/google
```

백엔드 실행 전에 클라이언트 정보와 로그인 성공 후 돌아갈 프론트 주소를 설정합니다.

```bash
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export FRONTEND_URL="http://localhost:5173"
export DB_URL="jdbc:postgresql://localhost:5432/algopoolja"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
```

Cloud Run 운영 환경에서는 `FRONTEND_URL=https://algopoolja-fe.vercel.app`, `SESSION_COOKIE_SAME_SITE=none`, `SESSION_COOKIE_SECURE=true`를 사용합니다.

로그인 시작 경로는 `/oauth2/authorization/google`, 현재 사용자 조회는 `GET /api/auth/me`, 로그아웃은 `POST /api/auth/logout`입니다.

테스트와 실행 JAR 생성은 다음 명령을 사용합니다.

```bash
./gradlew test
./gradlew bootJar
```

### Docker

```bash
docker build -t algopoolja-be .
docker run --rm -p 8080:8080 \
  -e OPENAI_API_KEY \
  -e GOOGLE_CLIENT_ID \
  -e GOOGLE_CLIENT_SECRET \
  -e JWT_SECRET \
  -e DB_URL \
  -e DB_USERNAME \
  -e DB_PASSWORD \
  -e FRONTEND_URL=http://localhost:8080 \
  algopoolja-be
```

## API

- `POST /api/quizzes`: `{ "category": "DP", "difficulty": "보통" }`
- `GET /api/quizzes/{quizId}`: 기존 문제 재조회
- `POST /api/quizzes/{quizId}/check`: `{ "answers": ["..."], "startedAt": "2026-09-19T21:38:56+09:00" }`
- `GET /api/dashboard/summary`: 대시보드 통계, 최근 풀이, 복습 문제 통합 조회
- `GET /api/me/quiz-attempts`: 풀이 기록 페이지네이션 및 필터
- `GET /api/me/wrong-quizzes`: 오답·복습 문제 페이지네이션 및 필터
- `GET /api/auth/me`: 현재 Google 로그인 사용자 조회
- `POST /api/auth/logout`: 로그인 세션 종료

학습 API는 로그인 세션의 `userId`·`nickname` 또는 HS256 Bearer JWT의 `userId`·`nickname` claim으로 사용자를 식별합니다. JWT 사용 시 충분히 긴 `JWT_SECRET` 환경변수를 설정해야 합니다.

퀴즈, 테스트케이스, 사용자별 풀이 이력과 오답 상태는 PostgreSQL에 저장됩니다. 애플리케이션 시작 시 Flyway가 스키마를 생성하고 Hibernate는 스키마 일치 여부를 검증합니다. Google OAuth 최초 로그인 사용자는 이메일을 기준으로 자동 가입됩니다.

## 원본과의 대응

- Pydantic 구조화 출력 → Lombok DTO + `ChatClient.entity(...)`
- LangChain OpenAI 모델 → Spring AI OpenAI starter
- LangChain Tool → Spring AI `@Tool`
- `create_agent()` → 도구가 기본 등록된 Spring AI `ChatClient`
- Gradio State/UI → 서버 측 퀴즈 상태 + 정적 웹 UI
