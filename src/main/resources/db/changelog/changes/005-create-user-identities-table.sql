--liquibase formatted sql

--changeset rohit.kavthekar:005-create-user-identities-table
CREATE TABLE user_identities (
    id               BIGSERIAL    PRIMARY KEY,
    uuid             UUID         NOT NULL,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(50)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),

    CONSTRAINT uq_user_identities_provider
        UNIQUE (provider, provider_user_id),

    CONSTRAINT fk_user_identities_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);

--rollback DROP TABLE IF EXISTS user_identities;
