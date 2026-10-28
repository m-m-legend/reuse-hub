package io.github.mmlegend.reusehub.reservations.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.reservation.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class ExpirationJob {
    private static final Logger log = LoggerFactory.getLogger(ExpirationJob.class);
    private final ReservationService reservations;

    public ExpirationJob(ReservationService reservations) { this.reservations = reservations; }

    @Scheduled(fixedDelayString = "${app.reservation.expiration-delay-ms:60000}", initialDelay = 10000)
    public void run() {
        int expired = 0;
        for (var id : reservations.due()) {
            try {
                // Outra bean garante uma transação própria por reserva. Uma falha não reverte o lote.
                if (reservations.expireOne(id)) expired++;
            } catch (RuntimeException exception) {
                log.warn("Reservation expiration failed id={} type={}", id, exception.getClass().getSimpleName());
            }
        }
        if (expired > 0) log.info("Reservations expired count={}", expired);
    }
}
