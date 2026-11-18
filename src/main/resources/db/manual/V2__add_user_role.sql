ALTER TABLE user_entity
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'ADOPTER';

UPDATE user_entity
SET role = 'VETERINARIAN'
WHERE id IN (SELECT id FROM veterinarian_entity);
