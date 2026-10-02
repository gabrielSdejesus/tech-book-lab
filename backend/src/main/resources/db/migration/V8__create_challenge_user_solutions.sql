CREATE TABLE challenge_user_solutions (
    challenge_id VARCHAR(50) PRIMARY KEY REFERENCES challenges(id) ON DELETE CASCADE,
    code TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_challenge_user_solutions_challenge ON challenge_user_solutions(challenge_id);
