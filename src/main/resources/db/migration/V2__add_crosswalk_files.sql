ALTER TABLE schema_files RENAME TO mscr_files;
ALTER TABLE mscr_files RENAME COLUMN schema_pid TO pid;
ALTER TABLE mscr_files ADD COLUMN type VARCHAR(15);
UPDATE mscr_files SET type = 'SCHEMA' where type IS NULL;
ALTER TABLE mscr_files ALTER COLUMN type SET NOT NULL;