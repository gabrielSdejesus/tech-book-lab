-- V9__add_expected_reflection_to_challenges.sql
-- Adiciona coluna de gabarito/resposta de referência de trade-off aos desafios e traduções em inglês

ALTER TABLE challenges ADD COLUMN expected_reflection TEXT;

-- -------------------------------------------------------------
-- Seed Português (pt): Resposta Padrão / Gabarito de Trade-off
-- -------------------------------------------------------------
UPDATE challenges
SET expected_reflection = 'No modelo 3NF, os dados da empresa residem em uma única linha referenciada por chaves estrangeiras. A alteração do nome exige apenas um UPDATE atômico pontual, garantindo consistência imediata sem anomalias. Já no modelo desnormalizado (ex: documentos embutidos ou tabelas com redundância), o nome está replicado em múltiplos registros, exigindo varredura e atualização em massa, com risco de escrita parcial e inconsistência transitória.'
WHERE id = 'lab-01-ch-1';

UPDATE challenges
SET expected_reflection = 'A localidade de leitura otimiza buscas onde o documento inteiro é consumido junto, eliminando JOINs e acessos aleatórios a disco. O trade-off é a penalidade de atualização (write amplification): se uma informação compartilhada for duplicada em vários documentos, qualquer alteração exige reescrever cada documento individualmente, além de gerar overhead de armazenamento e risco de divergência.'
WHERE id = 'lab-01-ch-2';

UPDATE challenges
SET expected_reflection = 'Grafos tratam relacionamentos como entidades de primeira classe e possuem sintaxe declarativa em Cypher para caminhos de profundidade variável (ex: [:WITHIN*]). No modelo relacional, o desenvolvedor precisa construir CTEs recursivas complexas, controlar condições de parada e gerenciar junções sucessivas, o que eleva a carga cognitiva e a fragilidade da consulta.'
WHERE id = 'lab-02-ch-1';

UPDATE challenges
SET expected_reflection = 'Em grafos densos, CTEs recursivas sofrem com a explosão combinatória de caminhos e alto consumo de memória/disco na tabela de trabalho (working table temporária). Há risco constante de loops infinitos caso não haja controle de ciclos. Além disso, o otimizador relacional tem dificuldade para estimar cardinalidade recursiva, gerando planos subótimos.'
WHERE id = 'lab-02-ch-2';

UPDATE challenges
SET expected_reflection = 'O OLTP foca em transações rápidas e concorrentes de escrita pontual, onde normalizar evita anomalias de atualização e reduz locks. Já o Star Schema (OLAP) foca em leitura e agregação analítica massiva; desnormalizar dimensões reduz a profundidade dos JOINs com a tabela de fatos, simplifica consultas de BI e favorece varreduras contínuas.'
WHERE id = 'lab-03-ch-1';

UPDATE challenges
SET expected_reflection = 'Bancos colunares leem do disco estritamente as colunas presentes na consulta, poupando largura de banda de I/O em tabelas largas. Além disso, colunas contíguas do mesmo tipo permitem taxas altíssimas de compressão (Run-Length, Delta) e processamento vetorizado (SIMD), onde múltiplos valores são agregados por ciclo de CPU diretamente na memória cache.'
WHERE id = 'lab-03-ch-2';

UPDATE challenges
SET expected_reflection = 'A imutabilidade transforma o banco em um registro histórico determinístico onde nenhum fato é sobrescrito ou apagado. Conflitos de atualização perdida (lost updates) são eliminados no modelo de escrita append-only. A trilha de auditoria é nativa e qualquer estado pode ser reconstruído ou corrigido reprocessando o log retrospectivamente.'
WHERE id = 'lab-04-ch-1';

UPDATE challenges
SET expected_reflection = 'Ela separa explicitamente o modelo de comando (o log de eventos de escrita append-only, otimizado para registrar intenções de negócio com baixa latência) do modelo de consulta (tabelas de leitura ou visões materializadas desnormalizadas para consultas rápidas). O modelo de consulta é uma projeção derivada, assíncrona e descartável do log de eventos.'
WHERE id = 'lab-04-ch-2';

-- -------------------------------------------------------------
-- Seed Inglês (en): Catalog Translations
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('CHALLENGE', 'lab-01-ch-1', 'en', 'expected_reflection', 'In the 3NF model, company data resides in a single row referenced via foreign keys. Renaming requires a single atomic UPDATE, ensuring instant consistency without anomalies. In a denormalized model, the name is duplicated across multiple records or documents, requiring expensive mass updates prone to write skew and temporary inconsistencies.'),
('CHALLENGE', 'lab-01-ch-2', 'en', 'expected_reflection', 'Read locality optimizes access when entire records are consumed together, eliminating JOINs and scattered disk reads. The trade-off is write amplification: when shared data is embedded across multiple documents, any modification requires rewriting every individual document, increasing write overhead and divergence risk.'),
('CHALLENGE', 'lab-02-ch-1', 'en', 'expected_reflection', 'Property graphs treat relationships as first-class citizens, offering concise declarative syntax for variable-length paths (e.g., [:WITHIN*]). In relational databases, traversing variable depths requires complex recursive CTEs with joins and termination checks, increasing cognitive load and error susceptibility.'),
('CHALLENGE', 'lab-02-ch-2', 'en', 'expected_reflection', 'In dense graphs, recursive CTEs suffer from path explosion and high memory/temp disk consumption in the recursive working table. They require manual cycle tracking to avoid infinite loops, and relational query planners struggle with cardinality estimation, often picking suboptimal plans.'),
('CHALLENGE', 'lab-03-ch-1', 'en', 'expected_reflection', 'OLTP targets high-concurrency transactional writes where normalization prevents update anomalies and reduces locks. Star Schema (OLAP) targets read-heavy aggregations across millions of rows; denormalizing dimensions minimizes join complexity with the fact table, simplifies BI queries, and enables fast table scans.'),
('CHALLENGE', 'lab-03-ch-2', 'en', 'expected_reflection', 'Columnar stores read only the columns queried, saving massive I/O bandwidth compared to row-oriented engines. Furthermore, contiguous column arrays allow high compression ratios (RLE, Delta) and SIMD vectorized execution, aggregating multiple values per CPU cycle directly in cache.'),
('CHALLENGE', 'lab-04-ch-1', 'en', 'expected_reflection', 'Immutability creates a deterministic audit trail where past facts are never overwritten or deleted. Lost update anomalies are prevented in append-only write streams. It provides natural point-in-time auditing and allows replay to rebuild or fix derived state retrospectively.'),
('CHALLENGE', 'lab-04-ch-2', 'en', 'expected_reflection', 'It decouples the command model (append-only event store capturing business intent with low write latency) from the query model (denormalized read tables or materialized views optimized for querying). The read model is an asynchronous, disposable projection derived from the immutable event log.');
