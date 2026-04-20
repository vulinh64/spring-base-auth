--liquibase formatted sql
--changeset vulinh:20251209-0003
CREATE TABLE new_subscriber
(
    subscribed_user_id  UUID,
    action_user_id      UUID        NOT NULL,
    action_username     VARCHAR(100),
    subscribed_username VARCHAR(100),
    created_date_time   TIMESTAMPTZ(6),
    updated_date_time   TIMESTAMPTZ(6),
    event_id            UUID,
    "timestamp"         TIMESTAMPTZ(6),
    PRIMARY KEY (subscribed_user_id, action_user_id)
);