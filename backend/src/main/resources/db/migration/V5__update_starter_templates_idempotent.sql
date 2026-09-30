-- Migration V5: Atualizar starter_templates dos desafios SQL para garantir idempotencia com IF NOT EXISTS

UPDATE challenges
SET starter_template = '-- 1. Crie as tabelas normalizadas (usuarios, experiencias_profissionais, formacoes_academicas)
CREATE TABLE IF NOT EXISTS usuarios (
    id INT PRIMARY KEY,
    nome VARCHAR(255),
    bio VARCHAR(500)
);

-- Escreva aqui as tabelas dependentes e os INSERTs de teste...
'
WHERE id = 'lab-01-ch-1';

UPDATE challenges
SET starter_template = '-- Crie a tabela usuarios_documento (id, perfil JSONB)
CREATE TABLE IF NOT EXISTS usuarios_documento (
    id INT PRIMARY KEY,
    perfil JSONB
);

-- Insira perfis contendo arrays de experiências e formações...
'
WHERE id = 'lab-01-ch-2';

UPDATE challenges
SET starter_template = '-- Crie as tabelas de dimensões desnormalizadas
CREATE TABLE IF NOT EXISTS dim_tempo (
    id INT PRIMARY KEY,
    data_completa DATE,
    mes INT,
    ano INT,
    trimestre INT
);
-- Complete com dim_produto, dim_loja e fato_vendas...
'
WHERE id = 'lab-03-ch-1';

UPDATE challenges
SET starter_template = '-- Crie a tabela pedidos_eventos estritamente append-only
CREATE TABLE IF NOT EXISTS pedidos_eventos (
    id SERIAL PRIMARY KEY,
    pedido_id INT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    dados_evento TEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insira uma sequência realista de eventos para múltiplos pedidos...
'
WHERE id = 'lab-04-ch-1';
