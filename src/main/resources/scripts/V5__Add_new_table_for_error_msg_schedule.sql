CREATE TABLE schedule_message_error_queue
(
    message_id bigint primary key,
    chat_id    bigint    not null,
    owner_id   bigint    not null,
    error_text text,
    time       timestamp not null
);