-- Campos bancários opcionais na tabela contas.
-- Armazenados criptografados via AES-256/GCM (AesAttributeConverter).
-- Tamanho 512 acomoda o dado criptografado em Base64 (IV 12 bytes + tag 16 bytes + payload).
ALTER TABLE contas
    ADD COLUMN IF NOT EXISTS numero_conta VARCHAR(512),
    ADD COLUMN IF NOT EXISTS agencia      VARCHAR(512);
