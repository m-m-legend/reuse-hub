package io.github.mmlegend.reusehub.reservations.persistence;

import io.github.mmlegend.reusehub.reservations.domain.ReservationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationEventRepository extends JpaRepository<ReservationEvent, UUID> {
    List<ReservationEvent> findByReservationIdOrderBySequenceNumberAsc(UUID reservationId);
}
