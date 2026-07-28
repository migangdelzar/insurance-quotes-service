ALTER TABLE quotes ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
CREATE INDEX idx_quotes_user_id ON quotes (user_id);
