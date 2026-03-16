-- Adiciona coluna usuario_id à tabela contas para isolamento por usuário
ALTER TABLE contas ADD COLUMN usuario_id BIGINT;

-- Para dados existentes, remove registros sem dono (ou associe ao primeiro usuário se existir)
DELETE FROM contas WHERE usuario_id IS NULL;

-- Torna a coluna obrigatória e adiciona FK
ALTER TABLE contas ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE contas ADD CONSTRAINT fk_contas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;

-- Índice de performance
CREATE INDEX idx_contas_usuario ON contas (usuario_id);
