--liquibase formatted sql
--changeset vulinh:20260427-0008

ALTER TABLE account_credential DROP CONSTRAINT fk_account_credential_account;

ALTER TABLE account_credential
    ADD CONSTRAINT fk_account_credential_account
        FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;
