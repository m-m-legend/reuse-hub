package io.github.mmlegend.reusehub.offers.persistence;

import io.github.mmlegend.reusehub.offers.domain.Offer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface OfferRepository extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offer o where o.id = :id")
    Optional<Offer> findLockedById(UUID id);
}
