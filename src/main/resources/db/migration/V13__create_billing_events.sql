CREATE TABLE billing_events (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(50)  NOT NULL, -- 'CLOUD_SQL_SUSPENDED', 'CLOUD_SQL_RESUMED', 'BUDGET_ALERT'
    service     VARCHAR(100) NOT NULL, -- ex: 'finance-db', 'finance-system'
    reason      TEXT,
    budget_pct  NUMERIC(5,2),          -- percentual do orçamento atingido (ex: 90.00)
    cost_usd    NUMERIC(10,2),         -- custo atual em USD no momento do alerta
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_events_created_at ON billing_events (created_at DESC);
CREATE INDEX idx_billing_events_event_type ON billing_events (event_type);
