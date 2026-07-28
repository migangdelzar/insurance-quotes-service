ALTER TABLE event_publication
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

ALTER TABLE event_publication
    ADD COLUMN IF NOT EXISTS completion_attempts INTEGER;

ALTER TABLE event_publication
    ADD COLUMN IF NOT EXISTS last_resubmission_date TIMESTAMPTZ;

UPDATE event_publication
SET status = CASE
                 WHEN completion_date IS NULL THEN 'PUBLISHED'
                 ELSE 'COMPLETED'
             END
WHERE status IS NULL;

UPDATE event_publication
SET completion_attempts = 0
WHERE completion_attempts IS NULL;

ALTER TABLE event_publication
    ALTER COLUMN status SET DEFAULT 'PUBLISHED',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN completion_attempts SET DEFAULT 0,
    ALTER COLUMN completion_attempts SET NOT NULL;
