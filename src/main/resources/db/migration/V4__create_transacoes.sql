CREATE TABLE transacoes (
    id           BIGSERIAL        PRIMARY KEY,
    descricao    VARCHAR(255)     NOT NULL,
    valor        NUMERIC(15, 2)   NOT NULL,
    tipo         VARCHAR(10)      NOT NULL,
    data         TIMESTAMP        NOT NULL,
    categoria_id BIGINT           REFERENCES categorias(id) ON DELETE SET NULL,
    usuario_id   BIGINT           NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX idx_transacoes_usuario ON transacoes (usuario_id);
CREATE INDEX idx_transacoes_data    ON transacoes (data DESC);