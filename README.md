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

## 시스템 구성

프론트엔드는 Vue.js와 Vercel을 사용하고, 백엔드는 Spring Boot 기반으로
Google Cloud Run에 배포했습니다. 문제와 사용자별 풀이 이력은 Cloud SQL(PostgreSQL)에
저장하며, Spring AI를 통해 OpenAI API와 연동합니다.

```text
Vue.js (Vercel)
       ↓
Spring Boot (Cloud Run)
       ├─ Spring AI / OpenAI API
       └─ Cloud SQL (PostgreSQL)
```

## AI 문제 생성 파이프라인

사용자가 알고리즘 유형·난이도·구현 언어를 선택하면 다음 과정을 거쳐 문제가 생성됩니다.

1. System Message로 문제 생성 규칙과 품질 기준을 전달합니다.
2. User Message로 사용자가 선택한 유형·난이도·언어를 전달합니다.
3. LLM이 문제 설명, 테스트케이스, 정답 코드를 구조화된 출력으로 생성합니다.
4. 정답 코드의 핵심 로직을 `<BLANK>`으로 변환합니다.
5. 서버가 원본 코드와 빈칸 코드를 비교해 정답 블록을 추출하고 생성 결과를 검증합니다.
6. 문제와 풀이 이력을 PostgreSQL에 저장합니다.

빈칸은 함수 시그니처나 단순 변수명이 아니라 반복 범위, 종료 조건,
상태 전이식, 비교식, 자료구조 연산처럼 알고리즘 이해가 필요한 핵심 로직을 대상으로 합니다.
문제 코드에 예외 분기가 포함된 경우 해당 조건과 반환값이 문제 설명과 테스트케이스에도 포함되도록 생성 규칙을 적용했습니다.

## 성능 개선

기존에는 문제 생성, 빈칸 생성, 정답·오답 선택지 생성 과정에서
AI API를 순차적으로 여러 번 호출했습니다. 호출 구조를 분석한 뒤,
정답 블록 추출과 단순 오답 생성은 서버 로직으로 전환하고,
독립적인 테스트케이스 정리 작업은 `CompletableFuture`로 병렬화했습니다.

동일한 `DP / 보통` 조건으로 운영 API에 10회 요청해 비교한 결과입니다.

| 항목 | 기존 구조 | 개선 구조 |
|---|---:|---:|
| 평균 응답 시간 | 17.265초 | 6.429초 |
| 중앙값 | 16.736초 | 5.888초 |
| 성공률 | 100% (10/10) | 90% (9/10) |
| 평균 응답 시간 개선율 | - | 62.7% |

개선 구조는 응답 시간을 크게 줄였지만 10회 중 1회 HTTP 502가 발생했습니다.
따라서 성능만을 기준으로 운영 기본값을 변경하지 않고, 안정적인 기존 구조를 유지하면서
실패 시 기존 AI 정답 생성 단계로 전환하는 fallback을 추가했습니다.

## 배포 및 장애 대응

Cloud Run과 Cloud SQL을 이용해 서비스를 배포하고 운영했습니다.
배포 과정에서 Cloud Build 서비스 계정의 Cloud Storage 객체 접근 권한 오류를
로그로 확인해 권한을 수정했습니다.

또한 AI가 빈칸 코드 생성 중 공백이나 줄바꿈을 변경해 서버의 정답 블록 추출이 실패하는
HTTP 502 문제를 분석했습니다. 로컬 추출에 실패하면 기존 `createAnswerBlocks()`를
호출하는 fallback을 추가해 응답 실패 대신 정상적인 문제 생성을 우선하도록 보완했습니다.

## 테스트

```bash
./gradlew test
```

빈칸 수와 정답 수 일치 여부, 코드 복원 가능 여부, 테스트케이스 정리,
문제 재사용과 풀이 이력 저장 흐름을 검증합니다.
