package io.github.mmlegend.reusehub.offers.application;

import io.github.mmlegend.reusehub.offers.domain.Offer;
import io.github.mmlegend.reusehub.offers.persistence.OfferRepository;
import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Contrato público de estoque: deve participar da transação do caso de uso. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class OfferInventory {
    private final OfferRepository offers;

    public OfferInventory(OfferRepository offers) { this.offers = offers; }

    public UUID lockAndGetOwner(UUID offerId) { return locked(offerId).getOwnerId(); }
    public void reserve(UUID offerId, int quantity) { locked(offerId).reserve(quantity); }
    public void release(UUID offerId, int quantity) { locked(offerId).release(quantity); }

    private Offer locked(UUID id) {
        return offers.findLockedById(id).orElseThrow(() -> BusinessException.notFound("Oferta"));
    }
}
