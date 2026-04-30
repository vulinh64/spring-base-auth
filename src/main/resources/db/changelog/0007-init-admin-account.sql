--liquibase formatted sql
--changeset vulinh:20260404-0007

INSERT INTO account (id, username, email, first_name, last_name, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000000', 'administrator', 'admin@localhost', 'Admin', 'User', 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO account_credential (id, account_id, credential_type, metadata, enabled, created_by, updated_by, created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'PASSWORD', '$2a$12$EE6IeEdWWxtl50izsia3muO6cihnAlDi2nGswxCSp/mhF..5KB/zO', TRUE, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO account_client_role (account_id, client_role_id)
VALUES ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000001');

INSERT INTO account_client_role (account_id, client_role_id)
VALUES ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000003');

INSERT INTO account_client_role (account_id, client_role_id)
VALUES ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000004');

INSERT INTO account_client_role (account_id, client_role_id)
VALUES ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000005');
