package io.github.mmlegend.reusehub.reservations.domain;

import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id private UUID id;
    @Column(name = "offer_id", nullable = false) private UUID offerId;
    @Column(name = "requester_id", nullable = false) private UUID requesterId;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(nullable = false) private int quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReservationStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected Reservation() {}

    public Reservation(UUID offerId, UUID requesterId, UUID ownerId, UUID idempotencyKey,
                       int quantity, Instant now, Instant expiresAt) {
        if (quantity < 1 || quantity > 10000 || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("Quantidade ou prazo inválido.");
        }
        if (requesterId.equals(ownerId)) {
            throw BusinessException.conflict("SELF_RESERVATION", "Você não pode reservar a própria oferta.");
        }
        this.id = UUID.randomUUID();
        this.offerId = offerId;
        this.requesterId = requesterId;
        this.ownerId = ownerId;
        this.idempotencyKey = idempotencyKey;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = now;
        this.updatedAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean collect(UUID actor, Instant now) {
        if (!ownerId.equals(actor)) throw BusinessException.forbidden();
        if (status == ReservationStatus.COLLECTED) return false;
        requireReserved();
        if (!now.isBefore(expiresAt)) {
            throw BusinessException.conflict("RESERVATION_EXPIRED", "O prazo de retirada terminou.");
        }
        transition(ReservationStatus.COLLECTED, now);
        return true;
    }

    public boolean cancel(UUID actor, Instant now) {
        if (!requesterId.equals(actor)) throw BusinessException.forbidden();
        if (status == ReservationStatus.CANCELLED) return false;
        requireReserved();
        transition(ReservationStatus.CANCELLED, now);
        return true;
    }

    public boolean expire(Instant now) {
        if (status != ReservationStatus.RESERVED || now.isBefore(expiresAt)) return false;
        transition(ReservationStatus.EXPIRED, now);
        return true;
    }

    public void requireParticipant(UUID actor) {
        if (!requesterId.equals(actor) && !ownerId.equals(actor)) throw BusinessException.notFound("Reserva");
    }

    public void requireSameRequest(UUID offer, int amount) {
        if (!offerId.equals(offer) || quantity != amount) {
            throw BusinessException.conflict("IDEMPOTENCY_KEY_REUSED", "A chave já foi usada com outro conteúdo.");
        }
    }

    private void requireReserved() {
        if (status != ReservationStatus.RESERVED) {
            throw BusinessException.conflict("INVALID_RESERVATION_STATE", "A reserva já foi finalizada.");
        }
    }

    private void transition(ReservationStatus next, Instant now) {
        status = next;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOfferId() { return offerId; }
    public UUID getRequesterId() { return requesterId; }
    public UUID getOwnerId() { return ownerId; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
