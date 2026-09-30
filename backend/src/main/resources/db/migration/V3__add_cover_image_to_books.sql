-- Migration V3: Add cover_image_url column to books table
ALTER TABLE books ADD COLUMN cover_image_url VARCHAR(500);

UPDATE books
SET cover_image_url = '/covers/ddia.svg'
WHERE id = 'ddia';
