-- Migrate transacoes.data from TIMESTAMP to DATE (time-of-day is not needed)
ALTER TABLE transacoes ALTER COLUMN data TYPE DATE USING data::DATE;
