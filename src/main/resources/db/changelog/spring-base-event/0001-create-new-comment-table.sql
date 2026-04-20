--liquibase formatted sql
--changeset vulinh:20251209-0000
CREATE TABLE new_comment
(
    id                UUID        NOT NULL PRIMARY KEY,
    content           TEXT,
    post_id           UUID,
    title             VARCHAR(200),
    excerpt           TEXT,
    created_date_time TIMESTAMPTZ(6),
    updated_date_time TIMESTAMPTZ(6),
    action_user_id    UUID,
    action_username   VARCHAR(100),
    status            VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    retry_count       INT         NOT NULL DEFAULT 0,
    failure_reason    TEXT,
    event_id          UUID,
    "timestamp"       TIMESTAMPTZ(6)
);