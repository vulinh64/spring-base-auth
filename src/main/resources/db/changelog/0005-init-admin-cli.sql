--liquibase formatted sql
--changeset vulinh:20260404-0005

INSERT INTO client (id, client_id, client_name, enabled, access_token_validity_seconds, refresh_token_validity_seconds, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000000', 'admin-cli', 'Admin CLI', TRUE, 3600, 86400, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'ADMIN', 'SYSTEM', 'SYSTEM',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', 'USER', 'SYSTEM', 'SYSTEM',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000', 'MANAGE_USERS', 'SYSTEM',
        'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000000', 'MANAGE_CLIENTS', 'SYSTEM',
        'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO client_role (id, client_id, role_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000000', 'MANAGE_CREDENTIALS', 'SYSTEM',
        'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
