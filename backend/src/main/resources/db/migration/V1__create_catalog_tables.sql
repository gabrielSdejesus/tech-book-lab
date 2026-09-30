CREATE TABLE books (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    tag_line VARCHAR(500),
    cover_color VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapters (
    id VARCHAR(50) PRIMARY KEY,
    book_id VARCHAR(50) NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    summary TEXT
);

CREATE TABLE labs (
    id VARCHAR(50) PRIMARY KEY,
    chapter_id VARCHAR(50) NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    number INT NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    engine_type VARCHAR(20) NOT NULL,
    database_name VARCHAR(50) NOT NULL,
    reset_schema_sql TEXT
);

CREATE TABLE lab_key_concepts (
    lab_id VARCHAR(50) NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    concept VARCHAR(100) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    PRIMARY KEY (lab_id, concept)
);

CREATE TABLE challenges (
    id VARCHAR(50) PRIMARY KEY,
    lab_id VARCHAR(50) NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scenario TEXT,
    starter_template TEXT,
    reflection_prompt TEXT
);

CREATE TABLE challenge_guidelines (
    challenge_id VARCHAR(50) NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
    guideline_text TEXT NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    PRIMARY KEY (challenge_id, order_index)
);
