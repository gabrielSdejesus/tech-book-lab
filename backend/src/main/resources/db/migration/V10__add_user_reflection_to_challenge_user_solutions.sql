-- Migration V10: Adiciona coluna user_reflection para persistir respostas aos questionários conceituais de trade-offs
ALTER TABLE challenge_user_solutions ADD COLUMN user_reflection TEXT;
