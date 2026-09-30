ALTER TABLE challenges ADD COLUMN engine_type VARCHAR(20);

UPDATE challenges SET engine_type = 'NEO4J' WHERE id = 'lab-02-ch-1';
UPDATE challenges SET engine_type = 'POSTGRES' WHERE id = 'lab-02-ch-2';
