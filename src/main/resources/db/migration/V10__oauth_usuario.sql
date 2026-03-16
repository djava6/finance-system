ALTER TABLE usuarios ALTER COLUMN senha DROP NOT NULL;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS provider VARCHAR(50);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_provider_id ON usuarios (provider, provider_id)
    WHERE provider IS NOT NULL;