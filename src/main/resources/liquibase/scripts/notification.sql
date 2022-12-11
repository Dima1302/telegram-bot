--liquibase formatted sql

--changeSet dmitriy:111
create table if not exists notification_task
(
    id           bigserial
    primary key,
    chat_id      bigint,
    notification text,
    first_name   text,
    last_name    text,
    date_time    timestamp
);
