create table users (
    id bigint generated always as identity primary key,
    email varchar(255) not null unique,
    nickname varchar(100) not null,
    created_at timestamptz not null default now()
);

create table quizzes (
    id bigint generated always as identity primary key,
    title varchar(300) not null,
    description text not null,
    category varchar(50) not null,
    difficulty varchar(20) not null,
    answer_code text not null,
    blank_code text not null,
    correct_answers jsonb not null,
    answer_blocks jsonb not null,
    content_hash varchar(128) not null unique,
    created_by bigint references users(id) on delete set null,
    created_at timestamptz not null default now(),
    constraint quizzes_difficulty_check check (difficulty in ('쉬움', '보통', '어려움'))
);

create table quiz_test_cases (
    id bigint generated always as identity primary key,
    quiz_id bigint not null references quizzes(id) on delete cascade,
    case_order int not null,
    input text not null,
    expected_output text not null,
    created_at timestamptz not null default now(),
    constraint quiz_test_cases_order_check check (case_order > 0),
    constraint quiz_test_cases_unique_order unique (quiz_id, case_order)
);

create table user_quizzes (
    id bigint generated always as identity primary key,
    user_id bigint not null references users(id) on delete cascade,
    quiz_id bigint not null references quizzes(id) on delete cascade,
    solved boolean not null default false,
    wrong_count int not null default 0,
    attempt_count int not null default 0,
    last_attempt_at timestamptz,
    next_review_at timestamptz,
    created_at timestamptz not null default now(),
    constraint user_quizzes_unique unique (user_id, quiz_id),
    constraint user_quizzes_wrong_count_check check (wrong_count >= 0),
    constraint user_quizzes_attempt_count_check check (attempt_count >= 0)
);

create table quiz_attempts (
    id bigint generated always as identity primary key,
    user_id bigint not null references users(id) on delete cascade,
    quiz_id bigint not null references quizzes(id) on delete cascade,
    answers jsonb not null,
    correct boolean not null,
    attempt_number int not null,
    started_at timestamptz,
    duration_seconds bigint,
    submitted_at timestamptz not null default now(),
    constraint quiz_attempts_number_check check (attempt_number > 0),
    constraint quiz_attempts_duration_check check (duration_seconds is null or duration_seconds >= 0)
);

create index idx_quizzes_category_difficulty on quizzes(category, difficulty);
create index idx_quiz_test_cases_quiz_id on quiz_test_cases(quiz_id, case_order);
create index idx_user_quizzes_user_review on user_quizzes(user_id, next_review_at);
create index idx_quiz_attempts_user_quiz on quiz_attempts(user_id, quiz_id, submitted_at);
