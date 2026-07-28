ALTER TABLE quotes ADD COLUMN user_id UUID;

UPDATE quotes
SET user_id = (
    SELECT id
    FROM users
    ORDER BY created_at, id
    LIMIT 1
)
WHERE user_id IS NULL;

ALTER TABLE quotes ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE quotes ADD CONSTRAINT fk_quotes_user_id FOREIGN KEY (user_id) REFERENCES users(id);
CREATE INDEX idx_quotes_user_id ON quotes (user_id);
