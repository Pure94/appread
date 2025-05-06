CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS appread;
SET search_path TO appread, public;

CREATE TABLE IF NOT EXISTS document_chunks (
                                               uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               project_id VARCHAR(255) NOT NULL,
                                               file_path VARCHAR(1024) NOT NULL,
                                               start_line INTEGER NOT NULL,
                                               end_line INTEGER NOT NULL,
                                               content TEXT NOT NULL,
                                               embedding vector(1536) NOT NULL,
                                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Zaktualizuj typ indeksu i opcje, jeśli to konieczne
-- Nazwa 'ivfflat' i 'vector_cosine_ops' są zazwyczaj w porządku
-- lists = 100 to rozsądny punkt wyjścia, można dostosować
CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx
    ON document_chunks
        USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
-- Lub użyj HNSW (często lepszy dla większych zbiorów danych):
-- CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx
--    ON document_chunks
--    USING hnsw (embedding vector_cosine_ops);


CREATE INDEX IF NOT EXISTS document_chunks_project_id_idx ON document_chunks (project_id);
CREATE INDEX IF NOT EXISTS document_chunks_file_path_idx ON document_chunks (file_path);

GRANT ALL PRIVILEGES ON SCHEMA appread TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA appread TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA appread TO postgres;