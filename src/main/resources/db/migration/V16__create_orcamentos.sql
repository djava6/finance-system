CREATE TABLE orcamentos (
    id           BIGSERIAL PRIMARY KEY,
    usuario_id   BIGINT NOT NULL REFERENCES usuarios(id),
    categoria_id BIGINT REFERENCES categorias(id),
    valor_limite DOUBLE PRECISION NOT NULL,
    mes          INTEGER NOT NULL,
    ano          INTEGER NOT NULL,
    UNIQUE (usuario_id, categoria_id, mes, ano)
);
