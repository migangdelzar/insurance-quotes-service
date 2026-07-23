CREATE TABLE quotes (
    id                            UUID PRIMARY KEY,
    name                          VARCHAR(120)  NOT NULL,
    email                         VARCHAR(254)  NOT NULL,
    age                           INTEGER       NOT NULL,
    zip_code                      VARCHAR(10)   NOT NULL,
    coverage_type                 VARCHAR(20),
    has_preexisting_conditions    BOOLEAN,
    takes_prescription_medication BOOLEAN,
    uses_tobacco                  BOOLEAN,
    needs_spouse_coverage         BOOLEAN,
    monthly_premium               NUMERIC(10, 2),
    status                        VARCHAR(30)   NOT NULL,
    created_at                    TIMESTAMPTZ   NOT NULL,
    updated_at                    TIMESTAMPTZ   NOT NULL,
    version                       BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE quote_health_conditions (
    quote_id  UUID        NOT NULL REFERENCES quotes (id),
    condition VARCHAR(30) NOT NULL
);

CREATE INDEX idx_quotes_status_created_at ON quotes (status, created_at);
