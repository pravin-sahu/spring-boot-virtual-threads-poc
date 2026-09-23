--liquibase formatted sql

--changeset rohit.kavthekar:001-create-roles-table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX idx_roles_name ON roles(name);
CREATE UNIQUE INDEX idx_roles_uuid ON roles(uuid);

--rollback DROP INDEX IF EXISTS idx_roles_uuid;
--rollback DROP INDEX IF EXISTS idx_roles_name;
--rollback DROP TABLE IF EXISTS roles;
