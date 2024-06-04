create table e_user_state
(
    user_id bigint not null,
    state   varchar(40),
    primary key (user_id)
);

create table e_message_state
(
    user_id bigint      not null,
    chat_id bigint      not null,
    state   varchar(40) not null,
    primary key (user_id, chat_id, state)
);

create table emessage_state_message_ids
(
    message_ids            integer,
    emessage_state_chat_id bigint      not null,
    emessage_state_user_id bigint      not null,
    emessage_state_state   varchar(40) not null
);

alter table emessage_state_message_ids
    add constraint FKogyci6s0u41bvtc3ybmjhkqlb foreign key (emessage_state_chat_id, emessage_state_user_id, emessage_state_state) references e_message_state;
