--liquibase formatted sql
--changeset vulinh:20260427-0009

CREATE INDEX idx_client_client_name ON client (client_name);
