--liquibase formatted sql
--changeset vulinh:20260404-0002

CREATE TABLE account_credential
(
    id                UUID        NOT NULL PRIMARY KEY,
    account_id        UUID,
    credential_type   VARCHAR(50) NOT NULL,
    metadata          VARCHAR(1024),
    enabled           BOOLEAN     NOT NULL,
    expires_at        TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    created_date_time TIMESTAMP WITH TIME ZONE,
    updated_date_time TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_account_credential UNIQUE (account_id, credential_type),
    CONSTRAINT fk_account_credential_account FOREIGN KEY (account_id) REFERENCES account (id)
);
