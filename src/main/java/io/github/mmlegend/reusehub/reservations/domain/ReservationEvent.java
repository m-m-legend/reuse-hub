package io.github.mmlegend.reusehub.reservations.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation_events")
public class ReservationEvent {
    @Id private UUID id;
    @Column(name = "reservation_id", nullable = false) private UUID reservationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReservationStatus status;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "sequence_number", nullable = false) private int sequenceNumber;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected ReservationEvent() {}

    public ReservationEvent(Reservation reservation, UUID actorId, Instant now) {
        this.id = UUID.randomUUID();
        this.reservationId = reservation.getId();
        this.status = reservation.getStatus();
        this.actorId = actorId;
        this.sequenceNumber = reservation.getStatus() == ReservationStatus.RESERVED ? 0 : 1;
        this.occurredAt = now;
    }

    public int getSequenceNumber() { return sequenceNumber; }
    public UUID getId() { return id; }
    public ReservationStatus getStatus() { return status; }
    public UUID getActorId() { return actorId; }
    public Instant getOccurredAt() { return occurredAt; }
}
