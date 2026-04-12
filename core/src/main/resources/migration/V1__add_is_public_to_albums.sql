-- Run this SQL on the production MySQL database before deploying
-- the new code version with isPublic support.

ALTER TABLE albums ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
