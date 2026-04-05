--liquibase formatted sql
--changeset vulinh:20260404-0003

CREATE TABLE client (
    id UUID NOT NULL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_name VARCHAR(255),
    enabled BOOLEAN NOT NULL,
    access_token_validity_seconds INT NOT NULL,
    refresh_token_validity_seconds INT NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_date_time TIMESTAMP WITH TIME ZONE,
    updated_date_time TIMESTAMP WITH TIME ZONE
);
