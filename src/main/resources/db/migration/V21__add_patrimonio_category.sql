INSERT INTO categorias (nome)
SELECT 'Patrimônio'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nome = 'Patrimônio');
