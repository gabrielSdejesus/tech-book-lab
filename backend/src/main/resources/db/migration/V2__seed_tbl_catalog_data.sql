-- Seed Book: Designing Data-Intensive Applications
INSERT INTO books (id, title, author, tag_line, cover_color, description)
VALUES (
    'ddia',
    'Designing Data-Intensive Applications',
    'Martin Kleppmann',
    'O guia definitivo para arquitetar sistemas distribuídos, confiáveis e escaláveis.',
    '#059669',
    'Aprenda na prática os trade-offs fundamentais por trás dos motores de banco de dados, índices, replicação, particionamento e consistência.'
);

-- Seed Chapter 3
INSERT INTO chapters (id, book_id, number, title, subtitle, summary)
VALUES (
    'ddia-cap-03',
    'ddia',
    3,
    'Modelos de Dados e Linguagens de Consulta',
    'Modelos de Dados, Grafos, OLAP e CQRS',
    'Explore as estruturas fundamentais que diferenciam bancos relacionais de documentos, travessia em grafos vs SQL recursivo, modelagem dimensional para analytics e CQRS baseado em log de eventos.'
);

-- Lab 1
INSERT INTO labs (id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql)
VALUES (
    'ddia-cap-03-lab-01',
    'ddia-cap-03',
    1,
    'relacional-vs-documentos',
    'Relacional vs Documentos e Localidade de Armazenamento',
    'Analise a incompatibilidade objeto-relacional (impedance mismatch) e o contraste entre tabelas 3NF e colunas JSONB.',
    'POSTGRES',
    'tbl_lab',
    'DROP TABLE IF EXISTS experiencias_profissionais CASCADE;
DROP TABLE IF EXISTS formacoes_academicas CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TABLE IF EXISTS usuarios_documento CASCADE;'
);

INSERT INTO lab_key_concepts (lab_id, concept, order_index) VALUES
('ddia-cap-03-lab-01', 'Impedance Mismatch', 1),
('ddia-cap-03-lab-01', 'Normalização 3NF', 2),
('ddia-cap-03-lab-01', 'Localidade de Armazenamento', 3),
('ddia-cap-03-lab-01', 'PostgreSQL JSONB', 4);

INSERT INTO challenges (id, lab_id, order_index, title, description, scenario, starter_template, reflection_prompt) VALUES
(
    'lab-01-ch-1',
    'ddia-cap-03-lab-01',
    1,
    'Modelagem 3NF (Relacional Estrito)',
    'Crie o modelo 3NF para o perfil profissional (estilo LinkedIn) com as tabelas normalizadas e popule dados de teste.',
    'Cada usuário tem nome, bio, múltiplas experiências profissionais (cargo, empresa, ano início e fim) e formações acadêmicas.',
    '-- 1. Crie as tabelas normalizadas (usuarios, experiencias_profissionais, formacoes_academicas)
CREATE TABLE usuarios (
    id INT PRIMARY KEY,
    nome VARCHAR(255),
    bio VARCHAR(500)
);

-- Escreva aqui as tabelas dependentes e os INSERTs de teste...
',
    'Se uma empresa mudar de nome institucional, qual o impacto no modelo 3NF comparado a um modelo desnormalizado?'
),
(
    'lab-01-ch-2',
    'ddia-cap-03-lab-01',
    2,
    'Modelagem Orientada a Documento (Postgres JSONB)',
    'Crie uma tabela com coluna JSONB e armazene todo o perfil de forma embutida aproveitando a localidade em disco.',
    'Compare a facilidade de busca direta por ID sem nenhum JOIN versus a complexidade de busca por elementos internos.',
    '-- Crie a tabela usuarios_documento (id, perfil JSONB)
CREATE TABLE usuarios_documento (
    id INT PRIMARY KEY,
    perfil JSONB
);

-- Insira perfis contendo arrays de experiências e formações...
',
    'Qual é o trade-off entre localidade de leitura (ler tudo de uma vez) e custo de atualização de um dado compartilhado?'
);

INSERT INTO challenge_guidelines (challenge_id, guideline_text, order_index) VALUES
('lab-01-ch-1', 'Modele chaves primárias e estrangeiras coerentes', 1),
('lab-01-ch-1', 'Popule ao menos 2 usuários com experiências e formações', 2),
('lab-01-ch-1', 'Escreva a query com LEFT JOIN para reconstruir o perfil completo e note a repetição dos campos do usuário', 3),
('lab-01-ch-2', 'Insira ao menos 2 perfis em formato JSONB estruturado', 1),
('lab-01-ch-2', 'Faça uma busca direta por ID e compare o plano com os múltiplos joins do modelo relacional', 2),
('lab-01-ch-2', 'Escreva uma consulta filtrando usuários que trabalharam em uma empresa específica usando operadores JSONB (ex: @> ou jsonb_array_elements)', 3);

-- Lab 2
INSERT INTO labs (id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql)
VALUES (
    'ddia-cap-03-lab-02',
    'ddia-cap-03',
    2,
    'grafos-propriedades',
    'Grafos de Propriedades vs SQL Recursivo',
    'Entenda como modelar relações complexas N:N e travessias com profundidade dinâmica em Neo4j vs PostgreSQL.',
    'NEO4J',
    'neo4j',
    'MATCH (n) DETACH DELETE n;'
);

INSERT INTO lab_key_concepts (lab_id, concept, order_index) VALUES
('ddia-cap-03-lab-02', 'Property Graphs', 1),
('ddia-cap-03-lab-02', 'Cypher', 2),
('ddia-cap-03-lab-02', 'Fechamento Transitivo', 3),
('ddia-cap-03-lab-02', 'CTE Recursiva SQL', 4);

INSERT INTO challenges (id, lab_id, order_index, title, description, scenario, starter_template, reflection_prompt) VALUES
(
    'lab-02-ch-1',
    'ddia-cap-03-lab-02',
    1,
    'Grafos de Propriedades com Cypher (Neo4j)',
    'Modele uma hierarquia geográfica de nós (Cidade -> Estado -> Região -> País) e conexões de amizade entre pessoas.',
    'Responda à pergunta: ''Quais amigos de Gabriel nasceram no Brasil?'' sem fixar a quantidade de saltos na hierarquia.',
    '// Crie os nós de Person e Location e as arestas [:WITHIN], [:BORN_IN], [:FRIENDS_WITH]
CREATE (c:Location {name: ''Sao Paulo'', type: ''Cidade''}),
       (s:Location {name: ''SP'', type: ''Estado''}),
       (b:Location {name: ''Brasil'', type: ''Pais''})
// Conecte com WITHIN e adicione Persons...
',
    'Por que o livro defende que modelos de grafos de propriedades são cognitivamente superiores para relações com número indefinido de saltos?'
),
(
    'lab-02-ch-2',
    'ddia-cap-03-lab-02',
    2,
    'SQL Recursivo (Postgres WITH RECURSIVE)',
    'Resolva o mesmo problema hierárquico no PostgreSQL utilizando uma tabela auto-relacionada e Common Table Expression recursiva.',
    'Descubra se Gabriel nasceu no Brasil utilizando busca recursiva ascendente.',
    '-- Tabela auto-relacionada para hierarquia
CREATE TABLE IF NOT EXISTS locais (
    id INT PRIMARY KEY,
    nome VARCHAR(100),
    parent_id INT REFERENCES locais(id)
);
-- Escreva a CTE WITH RECURSIVE...
',
    'Quais os desafios de performance e manutenção ao usar CTE recursiva para grafos densos em bancos relacionais?'
);

INSERT INTO challenge_guidelines (challenge_id, guideline_text, order_index) VALUES
('lab-02-ch-1', 'Utilize arestas direcionadas para representar continência geográfica', 1),
('lab-02-ch-1', 'Crie relacionamentos de amizade entre pessoas', 2),
('lab-02-ch-1', 'Escreva uma consulta Cypher usando operador de profundidade variável (*1..5 ou *) para encontrar o país', 3),
('lab-02-ch-2', 'Crie tabela de locais e pessoas associadas à sua cidade natal', 1),
('lab-02-ch-2', 'Escreva uma consulta recursiva que suba de filho a pai até o nó raiz', 2),
('lab-02-ch-2', 'Compare a legibilidade da query com a versão em Cypher', 3);

-- Lab 3
INSERT INTO labs (id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql)
VALUES (
    'ddia-cap-03-lab-03',
    'ddia-cap-03',
    3,
    'modelagem-dimensional-olap',
    'Modelagem Dimensional para Análise (Star Schema / OLAP)',
    'Descubra as diferenças de projeto entre bancos transacionais (OLTP) e sistemas analíticos (OLAP) com Star Schema.',
    'POSTGRES',
    'tbl_lab',
    'DROP TABLE IF EXISTS fato_vendas CASCADE;
DROP TABLE IF EXISTS dim_tempo CASCADE;
DROP TABLE IF EXISTS dim_produto CASCADE;
DROP TABLE IF EXISTS dim_loja CASCADE;'
);

INSERT INTO lab_key_concepts (lab_id, concept, order_index) VALUES
('ddia-cap-03-lab-03', 'OLTP vs OLAP', 1),
('ddia-cap-03-lab-03', 'Star Schema', 2),
('ddia-cap-03-lab-03', 'Tabelas de Fatos', 3),
('ddia-cap-03-lab-03', 'Tabelas de Dimensões', 4),
('ddia-cap-03-lab-03', 'Slice and Dice', 5);

INSERT INTO challenges (id, lab_id, order_index, title, description, scenario, starter_template, reflection_prompt) VALUES
(
    'lab-03-ch-1',
    'ddia-cap-03-lab-03',
    1,
    'Modelagem Dimensional (Star Schema)',
    'Crie as tabelas de dimensões desnormalizadas (dim_tempo, dim_produto, dim_loja) e a tabela de fatos central (fato_vendas).',
    'Construa o modelo de Data Warehouse voltado para análises rápidas cruzando tempo, produto e loja.',
    '-- Crie as tabelas de dimensões desnormalizadas
CREATE TABLE dim_tempo (
    id INT PRIMARY KEY,
    data_completa DATE,
    mes INT,
    ano INT,
    trimestre INT
);
-- Complete com dim_produto, dim_loja e fato_vendas...
',
    'Por que no OLTP normalizamos dados, enquanto no Star Schema desnormalizamos intencionalmente as tabelas de dimensão?'
),
(
    'lab-03-ch-2',
    'ddia-cap-03-lab-03',
    2,
    'Consultas Analíticas (Slice & Dice)',
    'Escreva consultas agregadoras típicas de BI sobre o Star Schema gerado.',
    'Agrupe faturamento total e quantidade vendida por ano, estado da loja e categoria de produto.',
    '-- Escreva a consulta analítica cruzando a tabela fato com as 3 dimensões
SELECT dt.ano, dl.estado, dp.categoria, 
       SUM(fv.valor_total) as faturamento_total,
       SUM(fv.quantidade) as total_itens
FROM fato_vendas fv
-- Complete os JOINs e GROUP BY...
',
    'Como os bancos analíticos colunares (ex: ClickHouse, DuckDB, Redshift) otimizam ainda mais esse tipo de consulta em relação ao Postgres por linha?'
);

INSERT INTO challenge_guidelines (challenge_id, guideline_text, order_index) VALUES
('lab-03-ch-1', 'Utilize chaves substitutas (surrogate keys) nas dimensões', 1),
('lab-03-ch-1', 'Na tabela fato, adicione chaves estrangeiras e métricas aditivas (quantidade, valor_total)', 2),
('lab-03-ch-1', 'Insira dados de teste simulando vendas em diferentes períodos e lojas', 3),
('lab-03-ch-2', 'Execute a query e analise o plano de execução (EXPLAIN ANALYZE)', 1),
('lab-03-ch-2', 'Observe a simplicidade dos JOINs em estrela contra a complexidade de navegar um modelo OLTP 3NF', 2),
('lab-03-ch-2', 'Teste a agregação com cláusula ROLLUP ou CUBE se desejar', 3);

-- Lab 4
INSERT INTO labs (id, chapter_id, number, slug, title, summary, engine_type, database_name, reset_schema_sql)
VALUES (
    'ddia-cap-03-lab-04',
    'ddia-cap-03',
    4,
    'cqrs-e-event-sourcing',
    'CQRS e Projeções Derivadas de um Log de Eventos',
    'Implemente o padrão de segregação de responsabilidade de leitura e escrita a partir de um log imutável de eventos.',
    'POSTGRES',
    'tbl_lab',
    'DROP MATERIALIZED VIEW IF EXISTS pedidos_resumo_leitura CASCADE;
DROP TABLE IF EXISTS pedidos_resumo_leitura CASCADE;
DROP TABLE IF EXISTS pedidos_eventos CASCADE;'
);

INSERT INTO lab_key_concepts (lab_id, concept, order_index) VALUES
('ddia-cap-03-lab-04', 'Event Sourcing', 1),
('ddia-cap-03-lab-04', 'CQRS', 2),
('ddia-cap-03-lab-04', 'Append-Only Log', 3),
('ddia-cap-03-lab-04', 'Materialized Views', 4),
('ddia-cap-03-lab-04', 'Consistência Eventual', 5);

INSERT INTO challenges (id, lab_id, order_index, title, description, scenario, starter_template, reflection_prompt) VALUES
(
    'lab-04-ch-1',
    'ddia-cap-03-lab-04',
    1,
    'Append-Only Event Store (Log de Eventos)',
    'Crie uma tabela de eventos imutáveis para registrar cada ação do ciclo de vida de um pedido.',
    'Grave eventos como PEDIDO_CRIADO, ITEM_ADICIONADO, DESCONTO_APLICADO, PAGAMENTO_CONFIRMADO e CANCELADO.',
    '-- Crie a tabela pedidos_eventos estritamente append-only
CREATE TABLE pedidos_eventos (
    id SERIAL PRIMARY KEY,
    pedido_id INT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    dados_evento TEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insira uma sequência realista de eventos para múltiplos pedidos...
',
    'Por que a imutabilidade do log de eventos simplifica auditoria e resolução de concorrência em sistemas distribuídos?'
),
(
    'lab-04-ch-2',
    'ddia-cap-03-lab-04',
    2,
    'Projeção Derivada Otimizada para Leitura (CQRS)',
    'Crie uma Materialized View ou tabela de projeção que consolide os eventos em um estado final pronto para leitura rápida.',
    'Simule um novo evento no log, atualize a projeção e comprove a velocidade de leitura direta.',
    '-- Crie uma tabela ou visão agregando os eventos do log
CREATE TABLE IF NOT EXISTS pedidos_resumo_leitura (
    pedido_id INT PRIMARY KEY,
    valor_total DECIMAL(10,2),
    status_final VARCHAR(50)
);
',
    'De que forma essa arquitetura atende à definição formal de CQRS apresentada no livro de Martin Kleppmann?'
);

INSERT INTO challenge_guidelines (challenge_id, guideline_text, order_index) VALUES
('lab-04-ch-1', 'Garanta que a tabela represente fatos passados no tempo', 1),
('lab-04-ch-1', 'Use payload estruturado para conter os dados de cada tipo de evento', 2),
('lab-04-ch-1', 'Simule múltiplos eventos evoluindo o estado do pedido', 3),
('lab-04-ch-2', 'Defina campos otimizados para exibição em tela (valor_total, status_final, itens)', 1),
('lab-04-ch-2', 'Simule a inserção de um evento de cancelamento ou pagamento no log', 2),
('lab-04-ch-2', 'Execute a consolidação e observe a consistência eventual', 3);
