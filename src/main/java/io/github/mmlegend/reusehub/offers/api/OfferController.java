package io.github.mmlegend.reusehub.offers.api;

import io.github.mmlegend.reusehub.offers.application.OfferService;
import io.github.mmlegend.reusehub.offers.domain.Category;
import io.github.mmlegend.reusehub.offers.domain.ItemCondition;
import io.github.mmlegend.reusehub.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/offers")
public class OfferController {
    private final OfferService offers;

    public OfferController(OfferService offers) { this.offers = offers; }

    @GetMapping
    public PageResponse<OfferService.OfferView> list(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) @Size(max = 100) String city,
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(defaultValue = "true") boolean availableOnly,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(offers.list(category, city, search, availableOnly, page, size));
    }

    @GetMapping("/{id}")
    public OfferService.OfferView get(@PathVariable UUID id) { return offers.get(id); }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OfferService.OfferView> create(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody CreateOfferRequest request) {
        var result = offers.create(UUID.fromString(jwt.getSubject()), request.title(), request.description(),
                request.category(), request.condition(), request.city(), request.quantity());
        return ResponseEntity.created(URI.create("/api/v1/offers/" + result.id())).body(result);
    }

    @PostMapping("/{id}/archive")
    @SecurityRequirement(name = "bearerAuth")
    public OfferService.OfferView archive(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return offers.archive(id, UUID.fromString(jwt.getSubject()));
    }

    public record CreateOfferRequest(@NotBlank @Size(max = 120) String title,
                                     @NotBlank @Size(max = 2000) String description,
                                     @NotNull Category category, @NotNull ItemCondition condition,
                                     @NotBlank @Size(max = 100) String city,
                                     @Min(1) @Max(10000) int quantity) {}
}
