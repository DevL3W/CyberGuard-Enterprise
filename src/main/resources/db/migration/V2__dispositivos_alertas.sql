CREATE TABLE devices (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies (id),
    name             VARCHAR(100) NOT NULL,
    hostname         VARCHAR(150),
    ip_address       VARCHAR(45),
    type             VARCHAR(20)  NOT NULL,
    operating_system VARCHAR(100),
    status           VARCHAR(20)  NOT NULL,
    last_seen        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_devices_company ON devices (company_id);

CREATE TABLE alerts (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT        NOT NULL REFERENCES companies (id),
    device_id   BIGINT        REFERENCES devices (id) ON DELETE SET NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    severity    VARCHAR(10)   NOT NULL,
    status      VARCHAR(12)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_alerts_company ON alerts (company_id, created_at);
