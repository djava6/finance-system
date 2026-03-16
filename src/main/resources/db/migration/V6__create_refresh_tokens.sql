CREATE TABLE refresh_tokens (
    id          BIGSERIAL   PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP    NOT NULL
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens (usuario_id);
