-- Demo seed data for local evaluation. Two tenants, one platform admin, one
-- tenant admin each, channel configs (all simulated), a rate limit, and a
-- template with a {{var}} placeholder. Passwords below are the PLAINTEXT
-- values corresponding to the BCrypt hashes inserted - use these to log in
-- via HTTP Basic:
--   platformadmin / PlatformAdmin@123
--   acme_admin    / TenantAdmin@123
--   globex_admin  / TenantAdmin@123

INSERT INTO tenant (name, created_at) VALUES ('Acme Corp', NOW());
SET @acme_id = LAST_INSERT_ID();

INSERT INTO tenant (name, created_at) VALUES ('Globex Inc', NOW());
SET @globex_id = LAST_INSERT_ID();

-- BCrypt hash of "PlatformAdmin@123"
INSERT INTO app_user (tenant_id, username, password_hash, role, created_at)
VALUES (NULL, 'platformadmin', '$2a$10$A9a7VHxJMb3ZXWXv8SVY0.ZG85tr1gl9uXPp9dboRodRscMK5E7Au', 'PLATFORM_ADMIN', NOW());

-- BCrypt hash of "TenantAdmin@123" (reused for both demo tenant admins)
INSERT INTO app_user (tenant_id, username, password_hash, role, created_at)
VALUES (@acme_id, 'acme_admin', '$2a$10$2c.MgN1Ls/a9hNWNRI3dIOhF7ggXUC0NhQurOv/DYrQqhimFmjQw2', 'TENANT_ADMIN', NOW());

INSERT INTO app_user (tenant_id, username, password_hash, role, created_at)
VALUES (@globex_id, 'globex_admin', '$2a$10$2c.MgN1Ls/a9hNWNRI3dIOhF7ggXUC0NhQurOv/DYrQqhimFmjQw2', 'TENANT_ADMIN', NOW());

INSERT INTO channel_config (tenant_id, channel_type, enabled, sender_identity, simulated_failure_rate) VALUES
    (@acme_id, 'EMAIL', TRUE, 'support@acme-corp.example', 0.1),
    (@acme_id, 'SMS', TRUE, 'ACMECO', 0.1),
    (@acme_id, 'PUSH', TRUE, 'Acme App', 0.1),
    (@acme_id, 'IN_APP', TRUE, NULL, 0.0),
    (@globex_id, 'EMAIL', TRUE, 'notify@globex.example', 0.2),
    (@globex_id, 'SMS', FALSE, 'GLOBEX', 0.2),
    (@globex_id, 'PUSH', TRUE, 'Globex App', 0.1),
    (@globex_id, 'IN_APP', TRUE, NULL, 0.0);

INSERT INTO rate_limit_config (tenant_id, capacity, refill_rate_per_sec) VALUES
    (@acme_id, 50, 5.0),
    (@globex_id, 20, 2.0);

INSERT INTO template (tenant_id, name, channel_type, content_template, created_at) VALUES
    (@acme_id, 'order_shipped', 'EMAIL', 'Hi {{name}}, your order {{orderId}} has shipped!', NOW()),
    (@globex_id, 'welcome_sms', 'SMS', 'Welcome to Globex, {{name}}! Reply STOP to opt out.', NOW());
