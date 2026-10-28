package io.github.mmlegend.reusehub.offers.domain;

import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class OfferTest {
    private final UUID owner = UUID.randomUUID();
    private Offer offer() {
        return new Offer(owner, "Kit ESP32", "Kit para laboratório", Category.ELECTRONICS,
                ItemCondition.GOOD, "Rio de Janeiro", 3, Instant.now());
    }

    @Test void reserveAndReleaseRespectStock() {
        var offer = offer();
        offer.reserve(2);
        assertThat(offer.getAvailableQuantity()).isEqualTo(1);
        assertThatThrownBy(() -> offer.reserve(2)).isInstanceOf(BusinessException.class);
        assertThat(offer.getAvailableQuantity()).isEqualTo(1);
        offer.release(2);
        assertThat(offer.getAvailableQuantity()).isEqualTo(3);
        assertThatThrownBy(() -> offer.release(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void archivedOfferStillAcceptsReleasedStock() {
        var offer = offer();
        offer.reserve(1);
        offer.archive(owner);
        assertThatThrownBy(() -> offer.reserve(1)).isInstanceOf(BusinessException.class);
        offer.release(1);
        assertThat(offer.getAvailableQuantity()).isEqualTo(3);
        assertThat(offer.isActive()).isFalse();
    }

    @Test void onlyOwnerCanArchive() {
        var offer = offer();
        assertThatThrownBy(() -> offer.archive(UUID.randomUUID())).isInstanceOf(BusinessException.class);
        assertThat(offer.isActive()).isTrue();
    }

    @ParameterizedTest @ValueSource(ints = {0, -1, 10001})
    void rejectsInvalidInitialQuantity(int quantity) {
        assertThatThrownBy(() -> new Offer(owner, "Kit", "Peças", Category.TOOLS,
                ItemCondition.GOOD, "Rio", quantity, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest @ValueSource(ints = {0, -1})
    void rejectsNonPositiveMovements(int quantity) {
        assertThatThrownBy(() -> offer().reserve(quantity)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> offer().release(quantity)).isInstanceOf(IllegalArgumentException.class);
    }
}
