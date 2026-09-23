--liquibase formatted sql

--changeset rohit.kavthekar:local-001-insert-sample-user context:local
INSERT INTO users (
    uuid,
    first_name,
    last_name,
    email,
    created_at,
    updated_at,
    created_by,
    updated_by
) VALUES (
    gen_random_uuid(),
    'Local',
    'Developer',
    'local.dev@example.com',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_identities (
    uuid,
    user_id,
    provider,
    provider_user_id,
    email,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    gen_random_uuid(),
    u.id,
    'LOCAL',
    'local.dev@example.com',
    'local.dev@example.com',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
FROM users u
WHERE u.email = 'local.dev@example.com'
ON CONFLICT (provider, provider_user_id) DO NOTHING;

--rollback DELETE FROM user_identities WHERE provider = 'LOCAL' AND provider_user_id = 'local.dev@example.com';
--rollback DELETE FROM users WHERE email = 'local.dev@example.com';
