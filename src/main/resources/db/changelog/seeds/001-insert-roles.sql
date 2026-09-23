--liquibase formatted sql

--changeset rohit.kavthekar:001-insert-roles
INSERT INTO roles (
    uuid,
    name,
    description,
    created_at,
    updated_at,
    created_by,
    updated_by
) VALUES (
    gen_random_uuid(),
    'ADMIN',
    'Default admin role',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (
    uuid,
    name,
    description,
    created_at,
    updated_at,
    created_by,
    updated_by
) VALUES (
    gen_random_uuid(),
    'USER',
    'Default user role',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system',
    'system'
)
ON CONFLICT (name) DO NOTHING;

--rollback DELETE FROM roles WHERE name IN ('ADMIN', 'USER');
