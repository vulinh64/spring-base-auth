--liquibase formatted sql
--changeset vulinh:20260404-0001

CREATE TABLE account
(
    id                UUID    NOT NULL PRIMARY KEY,
    username          VARCHAR(255),
    email             VARCHAR(255),
    first_name        VARCHAR(255),
    last_name         VARCHAR(255),
    is_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    created_date_time TIMESTAMP WITH TIME ZONE,
    updated_date_time TIMESTAMP WITH TIME ZONE
);
