--liquibase formatted sql
--changeset vulinh:20260404-0004

CREATE TABLE client_role
(
    id                UUID         NOT NULL PRIMARY KEY,
    client_id         UUID,
    role_name         VARCHAR(255) NOT NULL,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    created_date_time TIMESTAMP WITH TIME ZONE,
    updated_date_time TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_client_role UNIQUE (client_id, role_name),
    CONSTRAINT fk_client_role_client FOREIGN KEY (client_id) REFERENCES client (id)
);
