-- Migration V7: Remover comentários dos starter_templates e manter apenas código executável

UPDATE challenges
SET starter_template = 'CREATE TABLE IF NOT EXISTS usuarios (
    id INT PRIMARY KEY,
    nome VARCHAR(255),
    bio VARCHAR(500)
);'
WHERE id = 'lab-01-ch-1';

UPDATE challenges
SET starter_template = 'CREATE TABLE IF NOT EXISTS usuarios_documento (
    id INT PRIMARY KEY,
    perfil JSONB
);'
WHERE id = 'lab-01-ch-2';

UPDATE challenges
SET starter_template = 'CREATE (c:Location {name: ''Sao Paulo'', type: ''Cidade''}),
       (s:Location {name: ''SP'', type: ''Estado''}),
       (b:Location {name: ''Brasil'', type: ''Pais''});'
WHERE id = 'lab-02-ch-1';

UPDATE challenges
SET starter_template = 'CREATE TABLE IF NOT EXISTS locais (
    id INT PRIMARY KEY,
    nome VARCHAR(100),
    parent_id INT REFERENCES locais(id)
);'
WHERE id = 'lab-02-ch-2';

UPDATE challenges
SET starter_template = 'CREATE TABLE IF NOT EXISTS dim_tempo (
    id INT PRIMARY KEY,
    data_completa DATE,
    mes INT,
    ano INT,
    trimestre INT
);'
WHERE id = 'lab-03-ch-1';

UPDATE challenges
SET starter_template = 'SELECT dt.ano, dl.estado, dp.categoria, 
       SUM(fv.valor_total) as faturamento_total,
       SUM(fv.quantidade) as total_itens
FROM fato_vendas fv
JOIN dim_tempo dt ON fv.tempo_id = dt.id
JOIN dim_loja dl ON fv.loja_id = dl.id
JOIN dim_produto dp ON fv.produto_id = dp.id
GROUP BY dt.ano, dl.estado, dp.categoria;'
WHERE id = 'lab-03-ch-2';

UPDATE challenges
SET starter_template = 'CREATE TABLE IF NOT EXISTS pedidos_eventos (
    id SERIAL PRIMARY KEY,
    pedido_id INT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    dados_evento TEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);'
WHERE id = 'lab-04-ch-1';

UPDATE challenges
SET starter_template = 'CREATE TABLE IF NOT EXISTS pedidos_resumo_leitura (
    pedido_id INT PRIMARY KEY,
    valor_total DECIMAL(10,2),
    status_final VARCHAR(50)
);'
WHERE id = 'lab-04-ch-2';
