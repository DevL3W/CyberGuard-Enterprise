ALTER TABLE devices ADD COLUMN agent_key_hash VARCHAR(64);

CREATE TABLE vulnerabilities (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT        NOT NULL REFERENCES companies (id),
    device_id   BIGINT        REFERENCES devices (id) ON DELETE SET NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    cve         VARCHAR(30),
    severity    VARCHAR(10)   NOT NULL,
    status      VARCHAR(12)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fixed_at    TIMESTAMPTZ
);
CREATE INDEX idx_vulnerabilities_company ON vulnerabilities (company_id, created_at);

CREATE TABLE incidents (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT        NOT NULL REFERENCES companies (id),
    device_id   BIGINT        REFERENCES devices (id) ON DELETE SET NULL,
    assignee_id BIGINT        REFERENCES app_users (id) ON DELETE SET NULL,
    created_by  BIGINT        REFERENCES app_users (id) ON DELETE SET NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    severity    VARCHAR(10)   NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    closed_at   TIMESTAMPTZ
);
CREATE INDEX idx_incidents_company ON incidents (company_id, created_at);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT        NOT NULL REFERENCES companies (id),
    user_id     BIGINT,
    username    VARCHAR(60)   NOT NULL,
    action      VARCHAR(30)   NOT NULL,
    entity_type VARCHAR(30)   NOT NULL,
    entity_id   BIGINT,
    detail      VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_company ON audit_log (company_id, created_at);
