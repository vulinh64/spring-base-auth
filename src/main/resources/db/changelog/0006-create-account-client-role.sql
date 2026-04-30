--liquibase formatted sql
--changeset vulinh:20260404-0006

CREATE TABLE account_client_role (
    account_id UUID NOT NULL,
    client_role_id UUID NOT NULL,
    PRIMARY KEY (account_id, client_role_id),
    CONSTRAINT fk_acr_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_acr_client_role FOREIGN KEY (client_role_id) REFERENCES client_role (id)
);
