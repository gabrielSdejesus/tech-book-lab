-- =====================================================================
-- V11__interactive_scenarios_and_chapter_quizzes.sql
-- Cenários Interativos de Arquitetura & Simulados de Fixação de Capítulo (PT/EN)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Cenários Interativos de Decisão de Arquitetura (Canvas & Restrições)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interactive_scenarios (
    id VARCHAR(100) PRIMARY KEY,
    lab_id VARCHAR(50) NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    scenario_type VARCHAR(50) NOT NULL, -- CANVAS_MODELING, SLIDER_SIMULATION
    difficulty VARCHAR(20) NOT NULL,     -- EASY, MEDIUM, HARD
    order_index INT NOT NULL DEFAULT 1,
    constraints_json TEXT NOT NULL,
    initial_state_json TEXT NOT NULL,
    validation_rules_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS scenario_translations (
    scenario_id VARCHAR(100) NOT NULL REFERENCES interactive_scenarios(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    context_briefing TEXT NOT NULL,
    expected_reflection TEXT NOT NULL,
    PRIMARY KEY (scenario_id, locale)
);

CREATE TABLE IF NOT EXISTS user_scenario_solutions (
    scenario_id VARCHAR(100) PRIMARY KEY REFERENCES interactive_scenarios(id) ON DELETE CASCADE,
    user_state_json TEXT NOT NULL,
    user_reflection TEXT,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 2. Simulados de Fixação de Capítulo por Dificuldade
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chapter_quiz_questions (
    id VARCHAR(100) PRIMARY KEY,
    chapter_id VARCHAR(50) NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    order_index INT NOT NULL DEFAULT 1,
    difficulty VARCHAR(20) NOT NULL -- EASY, MEDIUM, HARD
);

CREATE TABLE IF NOT EXISTS quiz_question_translations (
    question_id VARCHAR(100) NOT NULL REFERENCES chapter_quiz_questions(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    prompt TEXT NOT NULL,
    explanation TEXT NOT NULL,
    PRIMARY KEY (question_id, locale)
);

CREATE TABLE IF NOT EXISTS quiz_options (
    id VARCHAR(100) PRIMARY KEY,
    question_id VARCHAR(100) NOT NULL REFERENCES chapter_quiz_questions(id) ON DELETE CASCADE,
    option_letter VARCHAR(5) NOT NULL, -- 'A', 'B', 'C', 'D'
    is_correct BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quiz_option_translations (
    option_id VARCHAR(100) NOT NULL REFERENCES quiz_options(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY (option_id, locale)
);

CREATE TABLE IF NOT EXISTS user_quiz_submissions (
    chapter_id VARCHAR(50) NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    question_id VARCHAR(100) NOT NULL REFERENCES chapter_quiz_questions(id) ON DELETE CASCADE,
    selected_option_id VARCHAR(100) NOT NULL REFERENCES quiz_options(id) ON DELETE CASCADE,
    is_correct BOOLEAN NOT NULL,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chapter_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_questions_chapter ON chapter_quiz_questions(chapter_id, order_index);
CREATE INDEX IF NOT EXISTS idx_user_quiz_submissions_chapter ON user_quiz_submissions(chapter_id);
CREATE INDEX IF NOT EXISTS idx_interactive_scenarios_lab ON interactive_scenarios(lab_id, order_index);

-- ---------------------------------------------------------------------
-- 3. Seed: Cenário Interativo do Lab 01 (Capítulo 3)
-- ---------------------------------------------------------------------
INSERT INTO interactive_scenarios (
    id, lab_id, scenario_type, difficulty, order_index,
    constraints_json, initial_state_json, validation_rules_json
) VALUES (
    'ddia-cap-03-lab-01-scenario-01',
    'ddia-cap-03-lab-01',
    'CANVAS_MODELING',
    'MEDIUM',
    1,
    '{"maxLatencyMs":15,"readThroughputQps":50000,"writeThroughputQps":200,"dataLocalityRequired":true,"strongConsistencyRequired":false,"maxPayloadKb":64}',
    '{"nodes":[{"id":"node-client","type":"CLIENT","label":"Client Apps"},{"id":"node-db-primary","type":"STORAGE_NODE","label":"Document Store (JSONB)"}],"edges":[{"from":"node-client","to":"node-db-primary","label":"Single-key document fetch"}]}',
    '{"forbiddenRelationships":["MANY_TO_MANY_EMBEDDED"],"recommendedModel":"DOCUMENT_WITH_LOCALITY","minCacheNodes":0}'
);

INSERT INTO scenario_translations (
    scenario_id, locale, title, description, context_briefing, expected_reflection
) VALUES
(
    'ddia-cap-03-lab-01-scenario-01',
    'pt',
    'Decisão de Modelagem: Perfil de Usuário com Alta Carga de Leitura',
    'Projete a arquitetura de armazenamento para perfis de usuários com 99% de operações de leitura pontual e dados aninhados (experiências, contatos e preferências).',
    'No DDIA Capítulo 3, Martin Kleppmann explica o Impedance Mismatch. Para estruturas em árvore (1:N) onde o documento é quase sempre acessado como um todo, um modelo de documento oferece melhor localidade de armazenamento do que tabelas normalizadas distribuídas por múltiplos JOINs.',
    'O modelo de documento garante que todos os dados do perfil residam em blocos contíguos de disco, eliminando JOINs em tempo de leitura. O trade-off é que atualizações pontuais exigem reescrever o documento inteiro ou suporte do motor a atualizações parciais eficientes.'
),
(
    'ddia-cap-03-lab-01-scenario-01',
    'en',
    'Modeling Decision: User Profile with Read-Heavy Workload',
    'Design the storage architecture for user profiles with 99% point read operations and nested data (experiences, contacts, and preferences).',
    'In DDIA Chapter 3, Martin Kleppmann discusses the Object-Relational Impedance Mismatch. For tree structures (1:N) where the document is almost always accessed in its entirety, a document model provides superior storage locality over normalized tables spread across multiple JOINs.',
    'The document model guarantees that all profile data resides in contiguous disk blocks, eliminating JOINs at read time. The trade-off is that partial updates require rewriting the whole document or reliance on engine-level partial update support.'
);

-- ---------------------------------------------------------------------
-- 4. Seed: Simulados do Capítulo 3 (ddia-cap-03)
-- ---------------------------------------------------------------------

-- Questão 1 (EASY)
INSERT INTO chapter_quiz_questions (id, chapter_id, order_index, difficulty)
VALUES ('ddia-cap-03-quiz-01', 'ddia-cap-03', 1, 'EASY');

INSERT INTO quiz_question_translations (question_id, locale, prompt, explanation) VALUES
('ddia-cap-03-quiz-01', 'pt',
 'Qual é a principal vantagem de um modelo de documento em relação a um modelo relacional normalizado para dados com estrutura em árvore (1:N)?',
 'No Capítulo 3, Kleppmann destaca que, se o aplicativo normalmente precisa de todo o documento de uma só vez, a localidade de armazenamento evita múltiplos JOINs e buscas aleatórias no disco.'),
('ddia-cap-03-quiz-01', 'en',
 'What is the primary advantage of a document model over a normalized relational model for tree-structured (1:N) data?',
 'In Chapter 3, Kleppmann highlights that if the application typically needs the entire document at once, storage locality avoids multiple JOINs and random disk seeks.');

INSERT INTO quiz_options (id, question_id, option_letter, is_correct) VALUES
('ddia-cap-03-quiz-01-opt-a', 'ddia-cap-03-quiz-01', 'A', 0),
('ddia-cap-03-quiz-01-opt-b', 'ddia-cap-03-quiz-01', 'B', 1),
('ddia-cap-03-quiz-01-opt-c', 'ddia-cap-03-quiz-01', 'C', 0),
('ddia-cap-03-quiz-01-opt-d', 'ddia-cap-03-quiz-01', 'D', 0);

INSERT INTO quiz_option_translations (option_id, locale, text) VALUES
('ddia-cap-03-quiz-01-opt-a', 'pt', 'Suporte nativo a transações distribuídas globais ACID.'),
('ddia-cap-03-quiz-01-opt-a', 'en', 'Native support for distributed global ACID transactions.'),
('ddia-cap-03-quiz-01-opt-b', 'pt', 'Melhor localidade de armazenamento, reduzindo a necessidade de múltiplos JOINs para carregar a entidade completa.'),
('ddia-cap-03-quiz-01-opt-b', 'en', 'Superior storage locality, reducing multiple JOIN operations to load the complete entity.'),
('ddia-cap-03-quiz-01-opt-c', 'pt', 'Eliminação total de redundância de dados em relacionamentos muitos-para-muitos (N:N).'),
('ddia-cap-03-quiz-01-opt-c', 'en', 'Total elimination of data redundancy in many-to-many (N:M) relationships.'),
('ddia-cap-03-quiz-01-opt-d', 'pt', 'Capacidade de executar consultas analíticas complexas sem varredura completa.'),
('ddia-cap-03-quiz-01-opt-d', 'en', 'Ability to execute complex analytical queries without full table scans.');

-- Questão 2 (EASY)
INSERT INTO chapter_quiz_questions (id, chapter_id, order_index, difficulty)
VALUES ('ddia-cap-03-quiz-02', 'ddia-cap-03', 2, 'EASY');

INSERT INTO quiz_question_translations (question_id, locale, prompt, explanation) VALUES
('ddia-cap-03-quiz-02', 'pt',
 'O que caracteriza a incompatibilidade de impedância (impedance mismatch) no desenvolvimento de software?',
 'A incompatibilidade de impedância refere-se à camada de tradução necessária entre o modelo de objetos na aplicação e tabelas relacionais em linhas e colunas.'),
('ddia-cap-03-quiz-02', 'en',
 'What characterizes the impedance mismatch in software engineering?',
 'Impedance mismatch refers to the translation layer required between in-memory object models and relational rows and columns.');

INSERT INTO quiz_options (id, question_id, option_letter, is_correct) VALUES
('ddia-cap-03-quiz-02-opt-a', 'ddia-cap-03-quiz-02', 'A', 0),
('ddia-cap-03-quiz-02-opt-b', 'ddia-cap-03-quiz-02', 'B', 1),
('ddia-cap-03-quiz-02-opt-c', 'ddia-cap-03-quiz-02', 'C', 0),
('ddia-cap-03-quiz-02-opt-d', 'ddia-cap-03-quiz-02', 'D', 0);

INSERT INTO quiz_option_translations (option_id, locale, text) VALUES
('ddia-cap-03-quiz-02-opt-a', 'pt', 'A discrepância de velocidade entre a CPU e o disco rígido magnético.'),
('ddia-cap-03-quiz-02-opt-a', 'en', 'The speed discrepancy between CPU cache and magnetic disks.'),
('ddia-cap-03-quiz-02-opt-b', 'pt', 'A desconexão entre o modelo de objetos na aplicação orientada a objetos e o modelo relacional de tabelas e linhas.'),
('ddia-cap-03-quiz-02-opt-b', 'en', 'The disconnect between the object-oriented application models and the relational tables and rows.'),
('ddia-cap-03-quiz-02-opt-c', 'pt', 'A diferença entre os protocolos de rede TCP e UDP em transmissões de streaming.'),
('ddia-cap-03-quiz-02-opt-c', 'en', 'The difference between TCP and UDP protocols in streaming workloads.'),
('ddia-cap-03-quiz-02-opt-d', 'pt', 'O descompasso temporal entre nós líderes e seguidos em replicação de dados assíncrona.'),
('ddia-cap-03-quiz-02-opt-d', 'en', 'The replication lag between leader and follower nodes in asynchronous replication.');

-- Questão 3 (MEDIUM)
INSERT INTO chapter_quiz_questions (id, chapter_id, order_index, difficulty)
VALUES ('ddia-cap-03-quiz-03', 'ddia-cap-03', 3, 'MEDIUM');

INSERT INTO quiz_question_translations (question_id, locale, prompt, explanation) VALUES
('ddia-cap-03-quiz-03', 'pt',
 'Em qual cenário a desnormalização de um documento começa a apresentar sérios trade-offs negativos de manutenção e consistência?',
 'Ao desnormalizar entidades com relacionamentos N:M, qualquer alteração exige varreduras e atualizações em massa em todos os documentos duplicados, arriscando anomalias de consistência.'),
('ddia-cap-03-quiz-03', 'en',
 'In which scenario does document denormalization introduce severe maintenance and consistency trade-offs?',
 'When denormalizing entities with N:M relationships, updates require widespread write-amplification across duplicated documents, risking consistency anomalies.');

INSERT INTO quiz_options (id, question_id, option_letter, is_correct) VALUES
('ddia-cap-03-quiz-03-opt-a', 'ddia-cap-03-quiz-03', 'A', 0),
('ddia-cap-03-quiz-03-opt-b', 'ddia-cap-03-quiz-03', 'B', 1),
('ddia-cap-03-quiz-03-opt-c', 'ddia-cap-03-quiz-03', 'C', 0),
('ddia-cap-03-quiz-03-opt-d', 'ddia-cap-03-quiz-03', 'D', 0);

INSERT INTO quiz_option_translations (option_id, locale, text) VALUES
('ddia-cap-03-quiz-03-opt-a', 'pt', 'Quando todos os dados pertencem estritamente a um relacionamento hierárquico pai-filho 1:N.'),
('ddia-cap-03-quiz-03-opt-a', 'en', 'When all data strictly belongs to a hierarchical 1:N parent-child relationship.'),
('ddia-cap-03-quiz-03-opt-b', 'pt', 'Quando surgem relacionamentos muitos-para-muitos (N:M) e dados duplicados precisam ser atualizados em múltiplos registros concorrentes.'),
('ddia-cap-03-quiz-03-opt-b', 'en', 'When many-to-many (N:M) relationships emerge and duplicated data must be updated across multiple concurrent records.'),
('ddia-cap-03-quiz-03-opt-c', 'pt', 'Quando o banco de dados é configurado em modo exclusivamente somente-leitura.'),
('ddia-cap-03-quiz-03-opt-c', 'en', 'When the database engine operates in strict read-only mode.'),
('ddia-cap-03-quiz-03-opt-d', 'pt', 'Quando o volume de escrita é significativamente menor que o volume de leitura analítica.'),
('ddia-cap-03-quiz-03-opt-d', 'en', 'When write throughput is significantly smaller than analytical read throughput.');

-- Questão 4 (MEDIUM)
INSERT INTO chapter_quiz_questions (id, chapter_id, order_index, difficulty)
VALUES ('ddia-cap-03-quiz-04', 'ddia-cap-03', 4, 'MEDIUM');

INSERT INTO quiz_question_translations (question_id, locale, prompt, explanation) VALUES
('ddia-cap-03-quiz-04', 'pt',
 'Ao modelar redes sociais complexas com conexões de profundidade arbitrária (ex: amigos de amigos de amigos), qual é o principal benefício de um modelo de grafos sobre SQL relacional?',
 'Linguagens de grafos como Cypher tornam percursos com profundidade dinâmica concisos e legíveis, enquanto em SQL relacional exigem WITH RECURSIVE complexos e difíceis de otimizar.'),
('ddia-cap-03-quiz-04', 'en',
 'When modeling complex social networks with arbitrary traversal depths (e.g., friends of friends), what is the core advantage of a graph model over relational SQL?',
 'Graph languages such as Cypher express dynamic-depth traversals concisely, whereas relational SQL requires verbose and hard-to-optimize WITH RECURSIVE CTEs.');

INSERT INTO quiz_options (id, question_id, option_letter, is_correct) VALUES
('ddia-cap-03-quiz-04-opt-a', 'ddia-cap-03-quiz-04', 'A', 0),
('ddia-cap-03-quiz-04-opt-b', 'ddia-cap-03-quiz-04', 'B', 1),
('ddia-cap-03-quiz-04-opt-c', 'ddia-cap-03-quiz-04', 'C', 0),
('ddia-cap-03-quiz-04-opt-d', 'ddia-cap-03-quiz-04', 'D', 0);

INSERT INTO quiz_option_translations (option_id, locale, text) VALUES
('ddia-cap-03-quiz-04-opt-a', 'pt', 'Grafos gravam dados mais rápido na memória RAM do que qualquer outro modelo.'),
('ddia-cap-03-quiz-04-opt-a', 'en', 'Graph engines write data faster to RAM than any other data model.'),
('ddia-cap-03-quiz-04-opt-b', 'pt', 'Consultas de travessia e fecho transitivo são expressas de forma concisa e natural, sem requerer CTEs recursivas prolixas ou explosão de auto-joins.'),
('ddia-cap-03-quiz-04-opt-b', 'en', 'Traversals and transitive closure are expressed concisely and naturally without verbose recursive CTEs or self-join explosions.'),
('ddia-cap-03-quiz-04-opt-c', 'pt', 'O modelo de grafos dispensa qualquer tipo de índice para busca inicial de nós.'),
('ddia-cap-03-quiz-04-opt-c', 'en', 'Graph models eliminate the need for indexes when discovering starting nodes.'),
('ddia-cap-03-quiz-04-opt-d', 'pt', 'Grafos não consomem espaço em disco para armazenar arestas e propriedades.'),
('ddia-cap-03-quiz-04-opt-d', 'en', 'Graph models do not allocate disk space to store relationships and edge properties.');

-- Questão 5 (HARD)
INSERT INTO chapter_quiz_questions (id, chapter_id, order_index, difficulty)
VALUES ('ddia-cap-03-quiz-05', 'ddia-cap-03', 5, 'HARD');

INSERT INTO quiz_question_translations (question_id, locale, prompt, explanation) VALUES
('ddia-cap-03-quiz-05', 'pt',
 'Por que bancos de dados orientados a documentos frequentemente enfrentam amplificação de escrita (write amplification) ao atualizar pequenos campos de documentos extensos?',
 'Kleppmann observa que, se uma atualização altera o tamanho do documento além do bloco alocado, o banco deve realocá-lo integralmente em um novo segmento de disco, reescrevendo todos os seus bytes e atualizando ponteiros de índices.'),
('ddia-cap-03-quiz-05', 'en',
 'Why do document databases often suffer from write amplification when updating small fields in large documents?',
 'Kleppmann notes that if an update exceeds the preallocated contiguous document slot, the engine must relocate the entire document to a new segment, rewriting all bytes and updating index pointers.');

INSERT INTO quiz_options (id, question_id, option_letter, is_correct) VALUES
('ddia-cap-03-quiz-05-opt-a', 'ddia-cap-03-quiz-05', 'A', 0),
('ddia-cap-03-quiz-05-opt-b', 'ddia-cap-03-quiz-05', 'B', 1),
('ddia-cap-03-quiz-05-opt-c', 'ddia-cap-03-quiz-05', 'C', 0),
('ddia-cap-03-quiz-05-opt-d', 'ddia-cap-03-quiz-05', 'D', 0);

INSERT INTO quiz_option_translations (option_id, locale, text) VALUES
('ddia-cap-03-quiz-05-opt-a', 'pt', 'Porque bancos de documentos não utilizam escrita sequencial em log (WAL).'),
('ddia-cap-03-quiz-05-opt-a', 'en', 'Because document databases do not employ write-ahead logging (WAL).'),
('ddia-cap-03-quiz-05-opt-b', 'pt', 'Porque muitos motores de documentos reescrevem o documento inteiro em disco quando seu tamanho cresce além do espaço contíguo alocado.'),
('ddia-cap-03-quiz-05-opt-b', 'en', 'Because many document engines rewrite the entire document to disk when its size expands beyond the allocated contiguous slot.'),
('ddia-cap-03-quiz-05-opt-c', 'pt', 'Porque índices secundários em documentos são proibidos pela especificação JSON.'),
('ddia-cap-03-quiz-05-opt-c', 'en', 'Because secondary indexes are strictly prohibited by the JSON specification.'),
('ddia-cap-03-quiz-05-opt-d', 'pt', 'Devido à necessidade de recalcular todas as chaves estrangeiras do banco.'),
('ddia-cap-03-quiz-05-opt-d', 'en', 'Due to the computational overhead of recalculating relational foreign keys.');

-- Questão 6 (HARD)
INSERT INTO chapter_quiz_questions (id, chapter_id, order_index, difficulty)
VALUES ('ddia-cap-03-quiz-06', 'ddia-cap-03', 6, 'HARD');

INSERT INTO quiz_question_translations (question_id, locale, prompt, explanation) VALUES
('ddia-cap-03-quiz-06', 'pt',
 'Em sistemas OLAP com modelagem dimensional (Star Schema), qual é a principal razão pela qual tabelas fato contêm centenas de milhões de linhas enquanto tabelas dimensão contêm muito menos?',
 'No Star Schema do Capítulo 3, cada linha da tabela fato representa um evento de negócio específico ocorrido no tempo. As tabelas dimensão fornecem as perspectivas de análise (atributos descritivos: quem, onde, o quê).'),
('ddia-cap-03-quiz-06', 'en',
 'In OLAP systems with dimensional modeling (Star Schema), why do fact tables contain hundreds of millions of rows while dimension tables are significantly smaller?',
 'In Star Schema (Chapter 3), each fact row captures a distinct discrete business event over time. Dimension tables provide the analytical perspectives and descriptive attributes (who, where, what).');

INSERT INTO quiz_options (id, question_id, option_letter, is_correct) VALUES
('ddia-cap-03-quiz-06-opt-a', 'ddia-cap-03-quiz-06', 'A', 0),
('ddia-cap-03-quiz-06-opt-b', 'ddia-cap-03-quiz-06', 'B', 1),
('ddia-cap-03-quiz-06-opt-c', 'ddia-cap-03-quiz-06', 'C', 0),
('ddia-cap-03-quiz-06-opt-d', 'ddia-cap-03-quiz-06', 'D', 0);

INSERT INTO quiz_option_translations (option_id, locale, text) VALUES
('ddia-cap-03-quiz-06-opt-a', 'pt', 'Tabelas dimensão armazenam eventos em tempo real, enquanto a tabela fato é estática.'),
('ddia-cap-03-quiz-06-opt-a', 'en', 'Dimension tables store real-time streaming events, whereas fact tables are static.'),
('ddia-cap-03-quiz-06-opt-b', 'pt', 'A tabela fato captura eventos individuais discretos ocorridos no tempo (ex: compras, cliques), enquanto as dimensões categorizam os atributos de contexto (quem, onde, o quê).'),
('ddia-cap-03-quiz-06-opt-b', 'en', 'The fact table captures discrete individual events over time (e.g., purchases, clicks), while dimensions categorize the context attributes (who, where, what).'),
('ddia-cap-03-quiz-06-opt-c', 'pt', 'Tabelas fato são índices invertidos construídos sobre as tabelas dimensão.'),
('ddia-cap-03-quiz-06-opt-c', 'en', 'Fact tables are inverted indexes built on top of dimension tables.'),
('ddia-cap-03-quiz-06-opt-d', 'pt', 'Tabelas dimensão não suportam colunas numéricas de agregação.'),
('ddia-cap-03-quiz-06-opt-d', 'en', 'Dimension tables do not support numeric metric aggregation columns.');
