package io.github.mmlegend.reusehub.reservations.domain;

import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReservationTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID requester = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-26T12:00:00Z");
    private Reservation reservation() {
        return new Reservation(UUID.randomUUID(), requester, owner, UUID.randomUUID(), 1, now, now.plusSeconds(3600));
    }

    @Test void collectionIsFinalAndRepeatedCollectionDoesNotChangeState() {
        var reservation = reservation();
        assertThat(reservation.collect(owner, now.plusSeconds(1))).isTrue();
        assertThat(reservation.collect(owner, now.plusSeconds(2))).isFalse();
        assertThat(reservation.getUpdatedAt()).isEqualTo(now.plusSeconds(1));
        assertThatThrownBy(() -> reservation.cancel(requester, now)).isInstanceOf(BusinessException.class);
        assertThat(reservation.expire(now.plusSeconds(7200))).isFalse();
    }

    @Test void cancellationIsIdempotentAndFinal() {
        var reservation = reservation();
        assertThat(reservation.cancel(requester, now)).isTrue();
        assertThat(reservation.cancel(requester, now)).isFalse();
        assertThatThrownBy(() -> reservation.collect(owner, now)).isInstanceOf(BusinessException.class);
    }

    @Test void expirationIncludesExactDeadline() {
        var reservation = reservation();
        assertThat(reservation.expire(now.plusSeconds(3599))).isFalse();
        assertThatThrownBy(() -> reservation.collect(owner, now.plusSeconds(3600))).isInstanceOf(BusinessException.class);
        assertThat(reservation.expire(now.plusSeconds(3600))).isTrue();
        assertThat(reservation.expire(now.plusSeconds(3601))).isFalse();
    }

    @Test void permissionsAreCheckedEvenForRepeatedActions() {
        var reservation = reservation();
        assertThatThrownBy(() -> reservation.collect(requester, now)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reservation.cancel(owner, now)).isInstanceOf(BusinessException.class);
        reservation.cancel(requester, now);
        assertThatThrownBy(() -> reservation.cancel(owner, now)).isInstanceOf(BusinessException.class);
    }

    @Test void cannotReserveOwnOffer() {
        assertThatThrownBy(() -> new Reservation(UUID.randomUUID(), owner, owner, UUID.randomUUID(),
                1, now, now.plusSeconds(1))).isInstanceOf(BusinessException.class);
    }

    @Test void idempotencyKeyCannotIdentifyAnotherPayload() {
        var reservation = reservation();
        reservation.requireSameRequest(reservation.getOfferId(), 1);
        assertThatThrownBy(() -> reservation.requireSameRequest(reservation.getOfferId(), 2))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reservation.requireSameRequest(UUID.randomUUID(), 1))
                .isInstanceOf(BusinessException.class);
    }
}
