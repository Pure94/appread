-- Add file_checksum column to document_chunks table
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS file_checksum VARCHAR(64);

-- Create document_files table for file metadata tracking
CREATE TABLE IF NOT EXISTS document_files (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    file_size BIGINT NOT NULL,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_files_project_file_unique UNIQUE (project_id, file_path)
);

-- Create indexes for document_files table
CREATE INDEX IF NOT EXISTS document_files_project_id_idx ON document_files (project_id);
CREATE INDEX IF NOT EXISTS document_files_file_path_idx ON document_files (file_path);
CREATE INDEX IF NOT EXISTS document_files_checksum_idx ON document_files (checksum);

-- Create index for file_checksum in document_chunks
CREATE INDEX IF NOT EXISTS document_chunks_file_checksum_idx ON document_chunks (file_checksum);

-- Grant privileges
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA appread TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA appread TO postgres;