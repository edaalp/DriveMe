-- 1:N vehicle documents; BYTEA storage for Neon/Hibernate (@JdbcTypeCode BINARY).
-- Migrates legacy vehicles.document_file whether stored as oid or bytea.

CREATE TABLE vehicle_documents (
    id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    file_name VARCHAR(512),
    document_type VARCHAR(32) NOT NULL,
    file_content BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_vehicle_documents PRIMARY KEY (id),
    CONSTRAINT fk_vehicle_documents_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE
);

CREATE INDEX idx_vehicle_documents_vehicle_id ON vehicle_documents (vehicle_id);

DO $$
DECLARE
    col_udt TEXT;
BEGIN
    SELECT c.udt_name
    INTO col_udt
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = 'vehicles'
      AND c.column_name = 'document_file';

    IF col_udt = 'oid' THEN
        INSERT INTO vehicle_documents (id, vehicle_id, file_name, document_type, file_content, created_at, updated_at, version)
        SELECT gen_random_uuid(),
               v.id,
               COALESCE(v.document_file_name, 'legacy.pdf'),
               'RUHSAT_FRONT',
               lo_get(v.document_file::oid),
               NOW(),
               NOW(),
               0
        FROM vehicles v
        WHERE v.document_file IS NOT NULL;
    ELSIF col_udt IS NOT NULL THEN
        INSERT INTO vehicle_documents (id, vehicle_id, file_name, document_type, file_content, created_at, updated_at, version)
        SELECT gen_random_uuid(),
               v.id,
               COALESCE(v.document_file_name, 'legacy.pdf'),
               'RUHSAT_FRONT',
               v.document_file,
               NOW(),
               NOW(),
               0
        FROM vehicles v
        WHERE v.document_file IS NOT NULL;
    END IF;
END $$;

ALTER TABLE vehicles DROP COLUMN IF EXISTS document_file;
ALTER TABLE vehicles DROP COLUMN IF EXISTS document_file_name;
