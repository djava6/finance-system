ALTER TABLE transacoes
    ADD COLUMN conta_id BIGINT REFERENCES contas(id) ON DELETE SET NULL;

CREATE INDEX idx_transacoes_conta ON transacoes(conta_id);
