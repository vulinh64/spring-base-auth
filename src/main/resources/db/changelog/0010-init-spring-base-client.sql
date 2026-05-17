--liquibase formatted sql
--changeset vulinh:20260404-0010

-- Dev-only service API key for spring-base → AS internal calls.
-- Plaintext: spring-base-local-dev-key-please-rotate-in-staging
-- SHA-256:   f6c3760d1a93b04bfeaf0544736104895d7b1307d5633f0aee88d12f34c6f1ff
-- The plaintext is committed only because this is a local-dev seed; rotate
-- before any non-local environment.
INSERT INTO client (id, client_id, client_name, enabled, access_token_validity_seconds, refresh_token_validity_seconds,
                    service_api_key_hash, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000010', 'spring-base', 'Spring Base', TRUE, 1800, 86400,
        'f6c3760d1a93b04bfeaf0544736104895d7b1307d5633f0aee88d12f34c6f1ff', 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000010', 'ADMIN', 'SYSTEM', 'SYSTEM',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000010', 'USER', 'SYSTEM', 'SYSTEM',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- POWER_USER sits between ADMIN and USER in the spring-base team's authorization hierarchy.
INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000010', 'POWER_USER', 'SYSTEM',
        'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
