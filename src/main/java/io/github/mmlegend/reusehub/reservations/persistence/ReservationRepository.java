package io.github.mmlegend.reusehub.reservations.persistence;

import io.github.mmlegend.reusehub.reservations.domain.Reservation;
import io.github.mmlegend.reusehub.reservations.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByRequesterIdAndIdempotencyKey(UUID requesterId, UUID idempotencyKey);

    interface Reference {
        UUID getOfferId();
        UUID getRequesterId();
        UUID getOwnerId();
    }

    @Query("select r.offerId as offerId, r.requesterId as requesterId, r.ownerId as ownerId from Reservation r where r.id = :id")
    Optional<Reference> findReferenceById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLockedById(UUID id);

    @Query("select r from Reservation r where (r.requesterId = :actor or r.ownerId = :actor) and (:status is null or r.status = :status)")
    Page<Reservation> findForParticipant(UUID actor, ReservationStatus status, Pageable pageable);

    @Query("select r.id from Reservation r where r.status = :status and r.expiresAt <= :now order by r.expiresAt, r.id")
    List<UUID> findDue(ReservationStatus status, Instant now, Pageable pageable);
}
