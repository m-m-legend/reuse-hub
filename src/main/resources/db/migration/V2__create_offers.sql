CREATE TABLE offers (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES accounts(id),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category VARCHAR(30) NOT NULL,
    item_condition VARCHAR(30) NOT NULL,
    city VARCHAR(100) NOT NULL,
    total_quantity INTEGER NOT NULL,
    available_quantity INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_offers_quantity CHECK (total_quantity BETWEEN 1 AND 10000),
    CONSTRAINT ck_offers_stock CHECK (available_quantity BETWEEN 0 AND total_quantity),
    CONSTRAINT ck_offers_category CHECK (category IN ('COMPUTERS', 'ELECTRONICS', 'TOOLS', 'EDUCATION', 'OTHER')),
    CONSTRAINT ck_offers_condition CHECK (item_condition IN ('NEW', 'GOOD', 'NEEDS_REPAIR'))
);
CREATE INDEX idx_offers_catalog ON offers(active, category, created_at, id);
CREATE INDEX idx_offers_owner ON offers(owner_id);
