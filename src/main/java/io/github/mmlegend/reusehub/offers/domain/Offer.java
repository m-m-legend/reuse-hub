package io.github.mmlegend.reusehub.offers.domain;

import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Category category;
    @Enumerated(EnumType.STRING) @Column(name = "item_condition", nullable = false, length = 30)
    private ItemCondition condition;
    @Column(nullable = false, length = 100) private String city;
    @Column(name = "total_quantity", nullable = false) private int totalQuantity;
    @Column(name = "available_quantity", nullable = false) private int availableQuantity;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected Offer() {}

    public Offer(UUID ownerId, String title, String description, Category category,
                 ItemCondition condition, String city, int quantity, Instant now) {
        if (quantity < 1 || quantity > 10000) throw new IllegalArgumentException("Quantidade deve estar entre 1 e 10000.");
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.title = title.strip();
        this.description = description.strip();
        this.category = category;
        this.condition = condition;
        this.city = city.strip();
        this.totalQuantity = quantity;
        this.availableQuantity = quantity;
        this.active = true;
        this.createdAt = now;
    }

    public void reserve(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantidade deve ser positiva.");
        if (!active) throw BusinessException.conflict("OFFER_ARCHIVED", "A oferta foi arquivada.");
        if (quantity > availableQuantity) throw BusinessException.conflict("INSUFFICIENT_STOCK", "Quantidade indisponível.");
        availableQuantity -= quantity;
    }

    public void release(int quantity) {
        if (quantity < 1 || quantity > totalQuantity - availableQuantity) {
            throw new IllegalArgumentException("Devolução de estoque inválida.");
        }
        availableQuantity += quantity;
    }

    public void archive(UUID actorId) {
        if (!ownerId.equals(actorId)) throw BusinessException.forbidden();
        active = false;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public ItemCondition getCondition() { return condition; }
    public String getCity() { return city; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
