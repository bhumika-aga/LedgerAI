-- Authentication schema (DATABASE §5.1 User, §5.9 RefreshToken, §6 Common Fields).
-- Additive, one logical change: the identity and refresh-token tables. No future tables (ADR-016).

-- Case-insensitive email (DATABASE §5.1): citext.
CREATE EXTENSION IF NOT EXISTS citext;

-- "user" is a reserved word in PostgreSQL; the singular table name is mandated by the naming
-- conventions (DATABASE §Naming Conventions → Tables), so it is quoted.
CREATE TABLE "user"
(
    id                   uuid        NOT NULL,
    email                citext      NOT NULL,
    password_hash        text        NOT NULL,
    full_name            text,
    professional_details text,
    preferences          jsonb,
    created_at           timestamptz NOT NULL,
    updated_at           timestamptz NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id),
    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE refresh_token
(
    id         uuid        NOT NULL,
    user_id    uuid        NOT NULL,
    token_hash text        NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);
