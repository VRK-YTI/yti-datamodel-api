-- Initial database schema for MSCR datamodel PI

CREATE TABLE IF NOT EXISTS schema_files (
	id serial PRIMARY KEY,
	schema_pid text NOT NULL,
	content_type text NOT NULL,
	data bytea NOT NULL
);

ALTER TABLE schema_files ALTER COLUMN data SET STORAGE EXTERNAL;