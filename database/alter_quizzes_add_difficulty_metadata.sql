alter table quizzes add column if not exists algorithm varchar(50);
alter table quizzes add column if not exists estimated_level varchar(20);
alter table quizzes add column if not exists time_complexity varchar(100);
alter table quizzes add column if not exists space_complexity varchar(100);
alter table quizzes add column if not exists required_techniques jsonb;

update quizzes
set algorithm = coalesce(algorithm, category),
    estimated_level = coalesce(estimated_level, '프로그래머스 Lv.2'),
    time_complexity = coalesce(time_complexity, '미상'),
    space_complexity = coalesce(space_complexity, '미상'),
    required_techniques = coalesce(required_techniques, '[]'::jsonb);

alter table quizzes alter column algorithm set not null;
alter table quizzes alter column estimated_level set not null;
alter table quizzes alter column time_complexity set not null;
alter table quizzes alter column space_complexity set not null;
alter table quizzes alter column required_techniques set not null;
