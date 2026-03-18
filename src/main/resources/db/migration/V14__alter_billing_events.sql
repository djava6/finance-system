ALTER TABLE billing_events
    ADD COLUMN triggered_by VARCHAR(50),  -- origem do evento: 'CloudFunction', 'Manual', 'GCPBilling'
    ADD COLUMN extra_info   JSONB;         -- dados adicionais flexíveis (ex: payload do Pub/Sub)
