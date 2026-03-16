-- Índice composto para queries de transações por usuário ordenadas por data
CREATE INDEX IF NOT EXISTS idx_transacoes_usuario_data ON transacoes (usuario_id, data DESC);

-- Índice em categoria para queries de despesas por categoria
CREATE INDEX IF NOT EXISTS idx_transacoes_categoria ON transacoes (categoria_id);

-- Índice em expiry_date para limpeza de refresh tokens expirados
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON refresh_tokens (expiry_date);
