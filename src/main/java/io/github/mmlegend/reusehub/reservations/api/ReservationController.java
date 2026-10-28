package io.github.mmlegend.reusehub.reservations.api;

import io.github.mmlegend.reusehub.reservations.application.ReservationService;
import io.github.mmlegend.reusehub.reservations.domain.ReservationStatus;
import io.github.mmlegend.reusehub.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) { this.reservations = reservations; }

    @PostMapping
    public ResponseEntity<ReservationService.ReservationView> create(
            @AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CreateReservationRequest request) {
        var result = reservations.create(actor(jwt), key, request.offerId(), request.quantity());
        return ResponseEntity.status(result.replayed() ? 200 : 201)
                .location(URI.create("/api/v1/reservations/" + result.reservation().id()))
                .header("Idempotency-Replayed", Boolean.toString(result.replayed())).body(result.reservation());
    }

    @GetMapping
    public PageResponse<ReservationService.ReservationView> list(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(reservations.list(actor(jwt), status, page, size));
    }

    @GetMapping("/{id}")
    public ReservationService.ReservationView get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reservations.get(id, actor(jwt));
    }

    @GetMapping("/{id}/events")
    public List<ReservationService.EventView> events(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reservations.history(id, actor(jwt));
    }

    @PostMapping("/{id}/cancel")
    public ReservationService.ReservationView cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reservations.cancel(id, actor(jwt));
    }

    @PostMapping("/{id}/collect")
    public ReservationService.ReservationView collect(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reservations.collect(id, actor(jwt));
    }

    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }

    public record CreateReservationRequest(@NotNull UUID offerId, @Min(1) @Max(10000) int quantity) {}
}
