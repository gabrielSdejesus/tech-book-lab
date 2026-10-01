-- V6__add_catalog_translations.sql
-- Tabela de traduções do catálogo e seed com conteúdo em inglês (en)

CREATE TABLE catalog_translations (
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    translation_text TEXT NOT NULL,
    PRIMARY KEY (entity_type, entity_id, locale, field_name)
);

CREATE INDEX idx_catalog_translations_lookup ON catalog_translations(locale, entity_type, entity_id);

-- -------------------------------------------------------------
-- Seed: Livros (Books)
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('BOOK', 'ddia', 'en', 'tag_line', 'The definitive guide to architecting distributed, reliable, and scalable systems.'),
('BOOK', 'ddia', 'en', 'description', 'Learn in practice the fundamental trade-offs behind database engines, indexes, replication, partitioning, and consistency.');

-- -------------------------------------------------------------
-- Seed: Capítulos (Chapters)
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('CHAPTER', 'ddia-cap-03', 'en', 'title', 'Data Models and Query Languages'),
('CHAPTER', 'ddia-cap-03', 'en', 'subtitle', 'Data Models, Graphs, OLAP and CQRS'),
('CHAPTER', 'ddia-cap-03', 'en', 'summary', 'Explore the foundational structures differentiating relational from document databases, graph traversals vs recursive SQL, dimensional modeling for analytics, and event-log-based CQRS.');

-- -------------------------------------------------------------
-- Seed: Laboratórios (Labs)
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('LAB', 'ddia-cap-03-lab-01', 'en', 'title', 'Relational vs Document and Storage Locality'),
('LAB', 'ddia-cap-03-lab-01', 'en', 'summary', 'Analyze object-relational impedance mismatch and the contrast between 3NF tables and JSONB columns.'),

('LAB', 'ddia-cap-03-lab-02', 'en', 'title', 'Property Graphs vs Recursive SQL'),
('LAB', 'ddia-cap-03-lab-02', 'en', 'summary', 'Understand how to model complex many-to-many relationships and dynamic-depth traversals in Neo4j vs PostgreSQL.'),

('LAB', 'ddia-cap-03-lab-03', 'en', 'title', 'Dimensional Modeling for Analytics (Star Schema / OLAP)'),
('LAB', 'ddia-cap-03-lab-03', 'en', 'summary', 'Discover design differences between transactional databases (OLTP) and analytical systems (OLAP) with Star Schema.'),

('LAB', 'ddia-cap-03-lab-04', 'en', 'title', 'CQRS and Projections Derived from an Event Log'),
('LAB', 'ddia-cap-03-lab-04', 'en', 'summary', 'Implement the Command Query Responsibility Segregation pattern from an immutable append-only event log.');

-- -------------------------------------------------------------
-- Seed: Conceitos-chave de Laboratórios (Lab Key Concepts)
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('LAB_CONCEPT', 'ddia-cap-03-lab-01:1', 'en', 'concept', 'Impedance Mismatch'),
('LAB_CONCEPT', 'ddia-cap-03-lab-01:2', 'en', 'concept', '3NF Normalization'),
('LAB_CONCEPT', 'ddia-cap-03-lab-01:3', 'en', 'concept', 'Storage Locality'),
('LAB_CONCEPT', 'ddia-cap-03-lab-01:4', 'en', 'concept', 'PostgreSQL JSONB'),

('LAB_CONCEPT', 'ddia-cap-03-lab-02:1', 'en', 'concept', 'Property Graphs'),
('LAB_CONCEPT', 'ddia-cap-03-lab-02:2', 'en', 'concept', 'Cypher'),
('LAB_CONCEPT', 'ddia-cap-03-lab-02:3', 'en', 'concept', 'Transitive Closure'),
('LAB_CONCEPT', 'ddia-cap-03-lab-02:4', 'en', 'concept', 'Recursive CTE SQL'),

('LAB_CONCEPT', 'ddia-cap-03-lab-03:1', 'en', 'concept', 'OLTP vs OLAP'),
('LAB_CONCEPT', 'ddia-cap-03-lab-03:2', 'en', 'concept', 'Star Schema'),
('LAB_CONCEPT', 'ddia-cap-03-lab-03:3', 'en', 'concept', 'Fact Tables'),
('LAB_CONCEPT', 'ddia-cap-03-lab-03:4', 'en', 'concept', 'Dimension Tables'),
('LAB_CONCEPT', 'ddia-cap-03-lab-03:5', 'en', 'concept', 'Slice and Dice'),

('LAB_CONCEPT', 'ddia-cap-03-lab-04:1', 'en', 'concept', 'Event Sourcing'),
('LAB_CONCEPT', 'ddia-cap-03-lab-04:2', 'en', 'concept', 'CQRS'),
('LAB_CONCEPT', 'ddia-cap-03-lab-04:3', 'en', 'concept', 'Append-Only Log'),
('LAB_CONCEPT', 'ddia-cap-03-lab-04:4', 'en', 'concept', 'Materialized Views'),
('LAB_CONCEPT', 'ddia-cap-03-lab-04:5', 'en', 'concept', 'Eventual Consistency');

-- -------------------------------------------------------------
-- Seed: Desafios (Challenges)
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('CHALLENGE', 'lab-01-ch-1', 'en', 'title', '3NF Modeling (Strict Relational)'),
('CHALLENGE', 'lab-01-ch-1', 'en', 'description', 'Create the 3NF model for a professional profile (LinkedIn style) with normalized tables and seed test data.'),
('CHALLENGE', 'lab-01-ch-1', 'en', 'scenario', 'Each user has a name, bio, multiple work experiences (position, company, start and end year), and education records.'),
('CHALLENGE', 'lab-01-ch-1', 'en', 'reflection_prompt', 'If a company changes its legal name, what is the impact on the 3NF model compared to a denormalized model?'),

('CHALLENGE', 'lab-01-ch-2', 'en', 'title', 'Document-Oriented Modeling (Postgres JSONB)'),
('CHALLENGE', 'lab-01-ch-2', 'en', 'description', 'Create a table with a JSONB column and store the entire profile embedded to leverage disk locality.'),
('CHALLENGE', 'lab-01-ch-2', 'en', 'scenario', 'Compare the ease of direct key-value lookup by ID without JOINs against the complexity of querying nested elements.'),
('CHALLENGE', 'lab-01-ch-2', 'en', 'reflection_prompt', 'What is the trade-off between read locality (reading everything at once) and the cost of updating shared data?'),

('CHALLENGE', 'lab-02-ch-1', 'en', 'title', 'Property Graphs with Cypher (Neo4j)'),
('CHALLENGE', 'lab-02-ch-1', 'en', 'description', 'Model a geographic hierarchy of nodes (City -> State -> Region -> Country) and friendship connections between people.'),
('CHALLENGE', 'lab-02-ch-1', 'en', 'scenario', 'Answer the question: ''Which friends of Gabriel were born in Brazil?'' without fixing the number of hops in the hierarchy.'),
('CHALLENGE', 'lab-02-ch-1', 'en', 'reflection_prompt', 'Why does the book argue that property graph models are cognitively superior for relations with an arbitrary number of hops?'),

('CHALLENGE', 'lab-02-ch-2', 'en', 'title', 'Recursive SQL (Postgres WITH RECURSIVE)'),
('CHALLENGE', 'lab-02-ch-2', 'en', 'description', 'Solve the same hierarchical problem in PostgreSQL using a self-referencing table and a recursive Common Table Expression.'),
('CHALLENGE', 'lab-02-ch-2', 'en', 'scenario', 'Find out if Gabriel was born in Brazil using an upward recursive search.'),
('CHALLENGE', 'lab-02-ch-2', 'en', 'reflection_prompt', 'What are the performance and maintenance challenges when using recursive CTEs for dense graphs in relational databases?'),

('CHALLENGE', 'lab-03-ch-1', 'en', 'title', 'Dimensional Modeling (Star Schema)'),
('CHALLENGE', 'lab-03-ch-1', 'en', 'description', 'Create denormalized dimension tables (dim_tempo, dim_produto, dim_loja) and the central fact table (fato_vendas).'),
('CHALLENGE', 'lab-03-ch-1', 'en', 'scenario', 'Build the Data Warehouse model designed for fast analytics slicing across time, product, and store.'),
('CHALLENGE', 'lab-03-ch-1', 'en', 'reflection_prompt', 'Why do we normalize data in OLTP, while intentionally denormalizing dimension tables in a Star Schema?'),

('CHALLENGE', 'lab-03-ch-2', 'en', 'title', 'Analytical Queries (Slice & Dice)'),
('CHALLENGE', 'lab-03-ch-2', 'en', 'description', 'Write typical BI aggregation queries on the generated Star Schema.'),
('CHALLENGE', 'lab-03-ch-2', 'en', 'scenario', 'Aggregate total revenue and quantity sold by year, store state, and product category.'),
('CHALLENGE', 'lab-03-ch-2', 'en', 'reflection_prompt', 'How do columnar analytics databases (e.g., ClickHouse, DuckDB, Redshift) optimize this query further compared to row-oriented Postgres?'),

('CHALLENGE', 'lab-04-ch-1', 'en', 'title', 'Append-Only Event Store (Event Log)'),
('CHALLENGE', 'lab-04-ch-1', 'en', 'description', 'Create an immutable event table to record each event in the lifecycle of an order.'),
('CHALLENGE', 'lab-04-ch-1', 'en', 'scenario', 'Record events such as ORDER_CREATED, ITEM_ADDED, DISCOUNT_APPLIED, PAYMENT_CONFIRMED, and CANCELLED.'),
('CHALLENGE', 'lab-04-ch-1', 'en', 'reflection_prompt', 'Why does event log immutability simplify auditing and concurrency resolution in distributed systems?'),

('CHALLENGE', 'lab-04-ch-2', 'en', 'title', 'Read-Optimized Derived Projection (CQRS)'),
('CHALLENGE', 'lab-04-ch-2', 'en', 'description', 'Create a Materialized View or projection table consolidating events into a final state ready for fast reads.'),
('CHALLENGE', 'lab-04-ch-2', 'en', 'scenario', 'Simulate a new log event, update the projection, and observe direct read performance.'),
('CHALLENGE', 'lab-04-ch-2', 'en', 'reflection_prompt', 'How does this architecture satisfy the formal definition of CQRS presented in Martin Kleppmann''s book?');

-- -------------------------------------------------------------
-- Seed: Diretrizes de Desafios (Challenge Guidelines)
-- -------------------------------------------------------------
INSERT INTO catalog_translations (entity_type, entity_id, locale, field_name, translation_text) VALUES
('GUIDELINE', 'lab-01-ch-1:1', 'en', 'guideline_text', 'Model coherent primary and foreign keys'),
('GUIDELINE', 'lab-01-ch-1:2', 'en', 'guideline_text', 'Populate at least 2 users with experiences and education'),
('GUIDELINE', 'lab-01-ch-1:3', 'en', 'guideline_text', 'Write the query with LEFT JOIN to rebuild the full profile and observe repeating user columns'),

('GUIDELINE', 'lab-01-ch-2:1', 'en', 'guideline_text', 'Insert at least 2 profiles in structured JSONB format'),
('GUIDELINE', 'lab-01-ch-2:2', 'en', 'guideline_text', 'Run a direct lookup by ID and compare the plan with the relational multiple joins'),
('GUIDELINE', 'lab-01-ch-2:3', 'en', 'guideline_text', 'Write a query filtering users who worked at a specific company using JSONB operators (e.g., @> or jsonb_array_elements)'),

('GUIDELINE', 'lab-02-ch-1:1', 'en', 'guideline_text', 'Use directed edges to represent geographic containment'),
('GUIDELINE', 'lab-02-ch-1:2', 'en', 'guideline_text', 'Create friendship relationships between people'),
('GUIDELINE', 'lab-02-ch-1:3', 'en', 'guideline_text', 'Write a Cypher query using variable-length path operator (*1..5 or *) to find the country'),

('GUIDELINE', 'lab-02-ch-2:1', 'en', 'guideline_text', 'Create a table of locations and people associated with their birthplace'),
('GUIDELINE', 'lab-02-ch-2:2', 'en', 'guideline_text', 'Write a recursive query traversing upward from child to parent up to the root node'),
('GUIDELINE', 'lab-02-ch-2:3', 'en', 'guideline_text', 'Compare query readability with the Cypher equivalent'),

('GUIDELINE', 'lab-03-ch-1:1', 'en', 'guideline_text', 'Use surrogate keys on dimensions'),
('GUIDELINE', 'lab-03-ch-1:2', 'en', 'guideline_text', 'In fact table, add foreign keys and additive metrics (quantity, total_value)'),
('GUIDELINE', 'lab-03-ch-1:3', 'en', 'guideline_text', 'Insert test data simulating sales across different periods and stores'),

('GUIDELINE', 'lab-03-ch-2:1', 'en', 'guideline_text', 'Run the query and inspect the execution plan (EXPLAIN ANALYZE)'),
('GUIDELINE', 'lab-03-ch-2:2', 'en', 'guideline_text', 'Observe the simplicity of star joins against navigating an OLTP 3NF model'),
('GUIDELINE', 'lab-03-ch-2:3', 'en', 'guideline_text', 'Test aggregation with ROLLUP or CUBE clause if desired'),

('GUIDELINE', 'lab-04-ch-1:1', 'en', 'guideline_text', 'Ensure table represents past facts in time'),
('GUIDELINE', 'lab-04-ch-1:2', 'en', 'guideline_text', 'Use structured payload to hold data for each event type'),
('GUIDELINE', 'lab-04-ch-1:3', 'en', 'guideline_text', 'Simulate multiple events evolving order state'),

('GUIDELINE', 'lab-04-ch-2:1', 'en', 'guideline_text', 'Define optimized fields for screen display (total_value, final_status, items)'),
('GUIDELINE', 'lab-04-ch-2:2', 'en', 'guideline_text', 'Simulate inserting a cancellation or payment event into the log'),
('GUIDELINE', 'lab-04-ch-2:3', 'en', 'guideline_text', 'Execute consolidation and observe eventual consistency');
