CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_accounts_email UNIQUE (email),
    CONSTRAINT ck_accounts_email_normalized CHECK (email = LOWER(email))
);
