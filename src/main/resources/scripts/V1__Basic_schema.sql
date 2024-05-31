create sequence e_category_seq start with 1 increment by 1;
create sequence e_message_entity_seq start with 1 increment by 1;
create sequence e_message_seq start with 1 increment by 1;
create table e_category
(
    created_date_time timestamp(6),
    id                bigint not null,
    owner_id          bigint,
    updated_date_time timestamp(6),
    name              varchar(255),
    primary key (id)
);
create table e_message
(
    execution_step           integer,
    created_date_time        timestamp(6),
    id                       bigint not null,
    message_id               bigint,
    next_execution_date_time timestamp(6),
    owner_id                 bigint,
    updated_date_time        timestamp(6),
    file_id                  varchar(255),
    file_type                varchar(255) check (file_type in ('PHOTO', 'DOCUMENT', 'VIDEO')),
    text                     TEXT,
    primary key (id)
);
create table e_message_entity
(
    e_message_id bigint,
    id           bigint not null,
    value        varchar(255),
    primary key (id)
);
create table e_user
(
    created_date_time timestamp(6),
    id                bigint not null,
    language_code     varchar(255),
    primary key (id)
);
create table message_category
(
    category_id bigint not null,
    message_id  bigint not null,
    primary key (category_id, message_id)
);
alter table if exists e_message_entity
    add constraint FK4arugnuw2i7dwtjig978t018o foreign key (e_message_id) references e_message;
alter table if exists message_category
    add constraint FK5eepumit9sg3fh16qt6igi52v foreign key (category_id) references e_category;
alter table if exists message_category
    add constraint FK2ffoq63rubk6acwlfc94k8sot foreign key (message_id) references e_message;
