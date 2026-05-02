--liquibase formatted sql
--changeset vulinh:20260404-0011

-- Default user for the spring-base team. Password is `123456`.
-- BCrypt hash is the same one used for `administrator` since the password matches.
INSERT INTO account (id, username, email, first_name, last_name, created_by, updated_by, created_date_time,
                     updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000020', 'user', 'user@localhost', 'Default', 'User', 'SYSTEM', 'SYSTEM',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO account_credential (id, account_id, credential_type, metadata, enabled, created_by, updated_by,
                                created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000020', 'PASSWORD',
        '$2a$12$EE6IeEdWWxtl50izsia3muO6cihnAlDi2nGswxCSp/mhF..5KB/zO', TRUE, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Grant USER role on spring-base to the default user.
-- The administrator account stays scoped to admin-cli only — wire it to
-- spring-base explicitly later if/when we need to test admin flows there.
INSERT INTO account_client_role (account_id, client_role_id)
VALUES ('00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000012');

-- Admin test user for the spring-base team. Same password (123456), distinct account.
-- Lets the team exercise admin-only endpoints without pinging us for a role grant.
INSERT INTO account (id, username, email, first_name, last_name, created_by, updated_by, created_date_time,
                     updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000022', 'admin-user', 'admin-user@localhost', 'Admin', 'User', 'SYSTEM',
        'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO account_credential (id, account_id, credential_type, metadata, enabled, created_by, updated_by,
                                created_date_time, updated_date_time)
VALUES ('00000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000022', 'PASSWORD',
        '$2a$12$EE6IeEdWWxtl50izsia3muO6cihnAlDi2nGswxCSp/mhF..5KB/zO', TRUE, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO account_client_role (account_id, client_role_id)
VALUES ('00000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000011');
