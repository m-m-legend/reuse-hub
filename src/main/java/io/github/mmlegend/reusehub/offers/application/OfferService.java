package io.github.mmlegend.reusehub.offers.application;

import io.github.mmlegend.reusehub.offers.domain.*;
import io.github.mmlegend.reusehub.offers.persistence.OfferRepository;
import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferService {
    private final OfferRepository offers;
    private final Clock clock;

    public OfferService(OfferRepository offers, Clock clock) {
        this.offers = offers;
        this.clock = clock;
    }

    @Transactional
    public OfferView create(UUID owner, String title, String description, Category category,
                            ItemCondition condition, String city, int quantity) {
        return OfferView.from(offers.save(new Offer(owner, title, description, category, condition, city, quantity, clock.instant())));
    }

    @Transactional(readOnly = true)
    public Page<OfferView> list(Category category, String city, String search, boolean availableOnly, int page, int size) {
        Specification<Offer> spec = (root, query, cb) -> cb.isTrue(root.get("active"));
        if (category != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.strip().toLowerCase(Locale.ROOT)));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.strip().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '!'));
        }
        if (availableOnly) spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("availableQuantity"), 0));
        return offers.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(OfferView::from);
    }

    @Transactional(readOnly = true)
    public OfferView get(UUID id) {
        return OfferView.from(offers.findById(id).orElseThrow(() -> BusinessException.notFound("Oferta")));
    }

    @Transactional
    public OfferView archive(UUID id, UUID actor) {
        Offer offer = offers.findLockedById(id).orElseThrow(() -> BusinessException.notFound("Oferta"));
        offer.archive(actor);
        return OfferView.from(offer);
    }

    public record OfferView(UUID id, UUID ownerId, String title, String description, Category category,
                            ItemCondition condition, String city, int totalQuantity, int availableQuantity,
                            boolean active, Instant createdAt) {
        static OfferView from(Offer offer) {
            return new OfferView(offer.getId(), offer.getOwnerId(), offer.getTitle(), offer.getDescription(),
                    offer.getCategory(), offer.getCondition(), offer.getCity(), offer.getTotalQuantity(),
                    offer.getAvailableQuantity(), offer.isActive(), offer.getCreatedAt());
        }
    }
}
