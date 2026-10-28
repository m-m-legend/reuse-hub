ALTER TABLE reservation_events ADD COLUMN sequence_number INTEGER;
UPDATE reservation_events SET sequence_number = CASE WHEN status = 'RESERVED' THEN 0 ELSE 1 END;
ALTER TABLE reservation_events ALTER COLUMN sequence_number SET NOT NULL;
ALTER TABLE reservation_events ADD CONSTRAINT ck_events_sequence CHECK (
    (status = 'RESERVED' AND sequence_number = 0) OR
    (status <> 'RESERVED' AND sequence_number = 1)
);
ALTER TABLE reservation_events ADD CONSTRAINT uk_events_sequence UNIQUE (reservation_id, sequence_number);
