--liquibase formatted sql

--changeset furkan:007-add-signature-batch-id
ALTER TABLE contract_schema.contract_signatures
    ADD COLUMN batch_id UUID;

-- Backfill: each existing row gets its own UUID so historical signatures
-- are effectively isolated batches (won't pollute new rounds' all-signed checks).
UPDATE contract_schema.contract_signatures
SET batch_id = gen_random_uuid()
WHERE batch_id IS NULL;

ALTER TABLE contract_schema.contract_signatures
    ALTER COLUMN batch_id SET NOT NULL;

CREATE INDEX idx_contract_signatures_batch
    ON contract_schema.contract_signatures(batch_id);
