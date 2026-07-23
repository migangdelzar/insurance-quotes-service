CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    family_id  UUID         NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE passkey_credentials (
    id              UUID PRIMARY KEY,
    user_id         UUID          NOT NULL REFERENCES users (id),
    credential_id   VARCHAR(400)  NOT NULL UNIQUE,
    public_key_cose BYTEA         NOT NULL,
    signature_count BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_passkey_credentials_user ON passkey_credentials (user_id);
