CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offers(id),
    requester_id UUID NOT NULL REFERENCES accounts(id),
    owner_id UUID NOT NULL REFERENCES accounts(id),
    idempotency_key UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_reservations_idempotency UNIQUE (requester_id, idempotency_key),
    CONSTRAINT ck_reservations_quantity CHECK (quantity BETWEEN 1 AND 10000),
    CONSTRAINT ck_reservations_participants CHECK (requester_id <> owner_id),
    CONSTRAINT ck_reservations_deadline CHECK (expires_at > created_at),
    CONSTRAINT ck_reservations_status CHECK (status IN ('RESERVED', 'COLLECTED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX idx_reservations_due ON reservations(status, expires_at, id);
CREATE INDEX idx_reservations_requester ON reservations(requester_id, created_at, id);
CREATE INDEX idx_reservations_owner ON reservations(owner_id, created_at, id);
CREATE INDEX idx_reservations_offer ON reservations(offer_id);

CREATE TABLE reservation_events (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    status VARCHAR(20) NOT NULL,
    actor_id UUID REFERENCES accounts(id),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_events_status CHECK (status IN ('RESERVED', 'COLLECTED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX idx_events_reservation ON reservation_events(reservation_id, occurred_at, id);
