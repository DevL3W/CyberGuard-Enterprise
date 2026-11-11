CREATE TABLE invitations (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies (id),
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    role            VARCHAR(20)  NOT NULL,
    max_uses        INT          NOT NULL,
    uses            INT          NOT NULL DEFAULT 0,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_by      BIGINT,
    created_by_name VARCHAR(60),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_invitations_company ON invitations (company_id, created_at);
