CREATE TABLE orcamentos (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id),
    categoria_id BIGINT REFERENCES categorias(id),
    valor_limite NUMERIC(10,2) NOT NULL,
    mes         SMALLINT NOT NULL CHECK (mes BETWEEN 1 AND 12),
    ano         SMALLINT NOT NULL,
    UNIQUE (usuario_id, categoria_id, mes, ano)
);
