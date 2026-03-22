ALTER TABLE transacoes
    ADD COLUMN recorrente         BOOLEAN DEFAULT FALSE,
    ADD COLUMN frequencia         VARCHAR(10),
    ADD COLUMN proxima_ocorrencia DATE;

CREATE INDEX idx_transacoes_proxima_ocorrencia
    ON transacoes (proxima_ocorrencia)
    WHERE recorrente = TRUE;
