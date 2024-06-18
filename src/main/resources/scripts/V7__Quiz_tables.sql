create sequence e_quiz_seq start with 1 increment by 1;
create sequence e_quiz_question_seq start with 1 increment by 1;

create table e_quiz
(
    id                 bigint not null primary key,
    owner_id           bigint,
    message_id         bigint,
    created_date_time  timestamp,
    finished_date_time timestamp,
    status             varchar(50)
);


create table e_quiz_question
(
    id                 bigint not null primary key,
    quiz_id            bigint references e_quiz (id) on delete cascade,
    text               varchar(255),
    type               varchar(50),
    status             varchar(50),
    correct_answer     varchar,
    user_answer        varchar,
    variants           varchar,
    created_date_time  timestamp,
    finished_date_time timestamp
);
