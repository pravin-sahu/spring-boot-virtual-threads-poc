--liquibase formatted sql

--changeset rohit.kavthekar:002-create-users-table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX idx_users_uuid ON users(uuid);
CREATE UNIQUE INDEX idx_users_email ON users(email);

--rollback DROP INDEX IF EXISTS idx_users_email;
--rollback DROP INDEX IF EXISTS idx_users_uuid;
--rollback DROP TABLE IF EXISTS users;
