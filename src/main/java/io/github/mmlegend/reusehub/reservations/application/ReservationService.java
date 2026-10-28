package io.github.mmlegend.reusehub.reservations.application;

import io.github.mmlegend.reusehub.identity.application.AccountAccess;
import io.github.mmlegend.reusehub.offers.application.OfferInventory;
import io.github.mmlegend.reusehub.reservations.domain.*;
import io.github.mmlegend.reusehub.reservations.persistence.*;
import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final ReservationEventRepository events;
    private final OfferInventory inventory;
    private final AccountAccess accounts;
    private final Clock clock;
    private final Duration ttl;

    public ReservationService(ReservationRepository reservations, ReservationEventRepository events,
                              OfferInventory inventory, AccountAccess accounts, Clock clock,
                              @Value("${app.reservation.ttl}") Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("TTL da reserva deve ser positivo.");
        this.reservations = reservations;
        this.events = events;
        this.inventory = inventory;
        this.accounts = accounts;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Transactional
    public CreationResult create(UUID actor, UUID key, UUID offerId, int quantity) {
        // A conta serializa a chave de idempotência inclusive entre ofertas diferentes.
        accounts.lock(actor);
        var existing = reservations.findByRequesterIdAndIdempotencyKey(actor, key);
        if (existing.isPresent()) {
            existing.get().requireSameRequest(offerId, quantity);
            return new CreationResult(ReservationView.from(existing.get()), true);
        }
        UUID owner = inventory.lockAndGetOwner(offerId);
        Instant now = clock.instant();
        var reservation = new Reservation(offerId, actor, owner, key, quantity, now, now.plus(ttl));
        inventory.reserve(offerId, quantity);
        reservations.save(reservation);
        events.save(new ReservationEvent(reservation, actor, now));
        return new CreationResult(ReservationView.from(reservation), false);
    }

    @Transactional
    public ReservationView cancel(UUID id, UUID actor) {
        Reservation reservation = locked(id, actor);
        Instant now = clock.instant();
        if (reservation.cancel(actor, now)) {
            inventory.release(reservation.getOfferId(), reservation.getQuantity());
            events.save(new ReservationEvent(reservation, actor, now));
        }
        return ReservationView.from(reservation);
    }

    @Transactional
    public ReservationView collect(UUID id, UUID actor) {
        Reservation reservation = locked(id, actor);
        Instant now = clock.instant();
        if (reservation.collect(actor, now)) events.save(new ReservationEvent(reservation, actor, now));
        return ReservationView.from(reservation);
    }

    @Transactional
    public boolean expireOne(UUID id) {
        var snapshot = reservations.findReferenceById(id);
        if (snapshot.isEmpty()) return false;
        // Toda transição bloqueia primeiro a oferta e depois a reserva.
        inventory.lockAndGetOwner(snapshot.get().getOfferId());
        Reservation reservation = reservations.findLockedById(id).orElseThrow();
        Instant now = clock.instant();
        if (!reservation.expire(now)) return false;
        inventory.release(reservation.getOfferId(), reservation.getQuantity());
        events.save(new ReservationEvent(reservation, null, now));
        return true;
    }

    @Transactional(readOnly = true)
    public List<UUID> due() {
        return reservations.findDue(ReservationStatus.RESERVED, clock.instant(), PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public ReservationView get(UUID id, UUID actor) { return ReservationView.from(visible(id, actor)); }

    @Transactional(readOnly = true)
    public Page<ReservationView> list(UUID actor, ReservationStatus status, int page, int size) {
        return reservations.findForParticipant(actor, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(ReservationView::from);
    }

    @Transactional(readOnly = true)
    public List<EventView> history(UUID id, UUID actor) {
        visible(id, actor);
        return events.findByReservationIdOrderBySequenceNumberAsc(id).stream()
                .map(e -> new EventView(e.getId(), e.getSequenceNumber(), e.getStatus(), e.getActorId(), e.getOccurredAt())).toList();
    }

    private Reservation visible(UUID id, UUID actor) {
        Reservation reservation = reservations.findById(id).orElseThrow(() -> BusinessException.notFound("Reserva"));
        reservation.requireParticipant(actor);
        return reservation;
    }

    private Reservation locked(UUID id, UUID actor) {
        // Projeção escalar: não carregar entidade mutável antes de aguardar o lock.
        var snapshot = reservations.findReferenceById(id).orElseThrow(() -> BusinessException.notFound("Reserva"));
        if (!snapshot.getRequesterId().equals(actor) && !snapshot.getOwnerId().equals(actor)) {
            throw BusinessException.notFound("Reserva");
        }
        inventory.lockAndGetOwner(snapshot.getOfferId());
        return reservations.findLockedById(id).orElseThrow(() -> BusinessException.notFound("Reserva"));
    }

    public record CreationResult(ReservationView reservation, boolean replayed) {}
    public record EventView(UUID id, int sequence, ReservationStatus status, UUID actorId, Instant occurredAt) {}
    public record ReservationView(UUID id, UUID offerId, UUID requesterId, UUID ownerId, int quantity,
                                  ReservationStatus status, Instant createdAt, Instant expiresAt, Instant updatedAt) {
        static ReservationView from(Reservation reservation) {
            return new ReservationView(reservation.getId(), reservation.getOfferId(), reservation.getRequesterId(),
                    reservation.getOwnerId(), reservation.getQuantity(), reservation.getStatus(),
                    reservation.getCreatedAt(), reservation.getExpiresAt(), reservation.getUpdatedAt());
        }
    }
}
