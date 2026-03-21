CREATE TABLE metas (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id),
    nome        VARCHAR(255) NOT NULL,
    valor_alvo  DOUBLE PRECISION NOT NULL,
    valor_atual DOUBLE PRECISION NOT NULL DEFAULT 0,
    prazo       DATE,
    concluida   BOOLEAN NOT NULL DEFAULT FALSE
);
