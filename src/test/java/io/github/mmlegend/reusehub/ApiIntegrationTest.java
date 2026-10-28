package io.github.mmlegend.reusehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmlegend.reusehub.identity.application.TokenService;
import io.github.mmlegend.reusehub.identity.domain.Account;
import io.github.mmlegend.reusehub.identity.persistence.AccountRepository;
import io.github.mmlegend.reusehub.offers.application.OfferService;
import io.github.mmlegend.reusehub.offers.domain.Category;
import io.github.mmlegend.reusehub.offers.domain.ItemCondition;
import io.github.mmlegend.reusehub.reservations.application.ReservationService;
import io.github.mmlegend.reusehub.reservations.domain.ReservationEvent;
import io.github.mmlegend.reusehub.reservations.persistence.ReservationEventRepository;
import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiIntegrationTest.TimeConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired TokenService tokens;
    @Autowired JwtEncoder encoder;
    @Autowired OfferService offers;
    @Autowired ReservationService reservations;
    @Autowired MutableClock clock;
    @MockitoSpyBean ReservationEventRepository events;
    private UUID owner;
    private UUID requester;
    private UUID other;
    private String hash;

    @BeforeAll void hashOnce() { hash = passwords.encode("Example-pass-2026"); }

    @BeforeEach void resetDatabase() {
        reset(events);
        clock.set(Instant.now());
        jdbc.update("delete from reservation_events");
        jdbc.update("delete from reservations");
        jdbc.update("delete from offers");
        jdbc.update("delete from accounts");
        owner = account("owner@example.test");
        requester = account("requester@example.test");
        other = account("other@example.test");
    }

    private UUID account(String email) {
        return accounts.saveAndFlush(new Account("Conta de teste", email, hash, clock.instant())).getId();
    }

    private UUID offer(int quantity) {
        return offers.create(owner, "Kit ESP32", "Sensores e placa para laboratório", Category.ELECTRONICS,
                ItemCondition.GOOD, "Rio de Janeiro", quantity).id();
    }

    private UUID reserve(UUID offerId, int quantity) {
        return reservations.create(requester, UUID.randomUUID(), offerId, quantity).reservation().id();
    }

    private MvcResult request(String method, String path, UUID actor, Object body, UUID key, int status) throws Exception {
        var builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        if (actor != null) builder.header("Authorization", "Bearer " + tokens.issue(actor).accessToken());
        if (body != null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
        if (key != null) builder.header("Idempotency-Key", key);
        return mvc.perform(builder).andExpect(status().is(status)).andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    @Test void registrationLoginAndMeUseRealJwt() throws Exception {
        var result = request("POST", "/api/v1/auth/register", null,
                Map.of("name", "Marina", "email", "MARINA@example.test", "password", "Example-pass-2026"), null, 201);
        assertThat(body(result).get("email").asText()).isEqualTo("marina@example.test");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", hash);
        var login = request("POST", "/api/v1/auth/login", null,
                Map.of("email", "marina@example.test", "password", "Example-pass-2026"), null, 200);
        String token = body(login).get("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Marina"));
        request("POST", "/api/v1/auth/register", null,
                Map.of("name", "Duplicada", "email", "Marina@example.test", "password", "Example-pass-2026"), null, 409);
    }

    @Test void wrongAndUnknownCredentialsHaveSameError() throws Exception {
        for (String email : List.of("owner@example.test", "missing@example.test")) {
            var response = request("POST", "/api/v1/auth/login", null,
                    Map.of("email", email, "password", "wrong-password"), null, 401);
            assertThat(body(response).get("code").asText()).isEqualTo("INVALID_CREDENTIALS");
        }
    }

    @Test void rejectsInvalidRegistrationAndUnknownProperties() throws Exception {
        var invalid = request("POST", "/api/v1/auth/register", null,
                Map.of("name", " ", "email", "invalid", "password", "short"), null, 400);
        assertThat(body(invalid).get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body(invalid).get("errors").size()).isEqualTo(3);
        assertThat(body(invalid).get("requestId").asText()).isEqualTo(invalid.getResponse().getHeader("X-Request-Id"));
        request("POST", "/api/v1/auth/register", null,
                Map.of("name", "Teste", "email", "test@example.test", "password", "Example-pass-2026", "role", "ADMIN"), null, 400);
        request("POST", "/api/v1/auth/register", null,
                Map.of("name", "Teste", "email", "test@example.test", "password", "é".repeat(40)), null, 400);
        assertThat(accounts.count()).isEqualTo(3);
    }

    @Test void privateRoutesRequireAuthentication() throws Exception {
        var result = request("GET", "/api/v1/reservations", null, null, null, 401);
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(body(result).get("requestId").asText()).isEqualTo(result.getResponse().getHeader("X-Request-Id"));
        request("POST", "/api/v1/offers", null, Map.of(), null, 401);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test void expiredWrongIssuerAndWrongAudienceTokensAreRejected() throws Exception {
        var now = Instant.now();
        for (int scenario = 0; scenario < 4; scenario++) {
            var claims = JwtClaimsSet.builder().issuer(scenario == 1 ? "another-issuer" : "reuse-hub")
                    .subject(scenario == 3 ? "not-a-uuid" : owner.toString())
                    .audience(List.of(scenario == 2 ? "another-api" : "reuse-hub-api"))
                    .issuedAt(now.minusSeconds(600)).expiresAt(scenario == 0 ? now.minusSeconds(300) : now.plusSeconds(600)).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }

    @Test void catalogIsPublicFilteredAndPaginated() throws Exception {
        offer(3);
        offers.create(owner, "Livro de Java", "Introdução", Category.EDUCATION, ItemCondition.GOOD, "Niterói", 1);
        mvc.perform(get("/api/v1/offers").param("category", "ELECTRONICS").param("city", "rio de janeiro").param("search", "ESP").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Kit ESP32"));
        assertThat(body(request("GET", "/api/v1/offers?size=1", null, null, null, 200)).get("totalPages").asInt()).isEqualTo(2);
        request("GET", "/api/v1/offers?size=101", null, null, null, 400);
        request("GET", "/api/v1/offers?page=-1", null, null, null, 400);
        request("GET", "/api/v1/offers?category=INVALID", null, null, null, 400);
    }

    @Test void catalogSearchTreatsSqlWildcardsAsLiteral() throws Exception {
        offer(1);
        mvc.perform(get("/api/v1/offers").param("search", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test void createOfferReturnsLocationAndValidatesPayload() throws Exception {
        var payload = Map.of("title", "Notebook", "description", "Para estudo", "category", "COMPUTERS",
                "condition", "NEEDS_REPAIR", "city", "Rio de Janeiro", "quantity", 2);
        var result = request("POST", "/api/v1/offers", owner, payload, null, 201);
        assertThat(result.getResponse().getHeader("Location")).endsWith(body(result).get("id").asText());
        request("POST", "/api/v1/offers", owner, Map.of("quantity", 0), null, 400);
    }

    @Test void completeHttpFlowAndHistory() throws Exception {
        UUID offerId = offer(2);
        var result = request("POST", "/api/v1/reservations", requester,
                Map.of("offerId", offerId, "quantity", 1), UUID.randomUUID(), 201);
        String id = body(result).get("id").asText();
        request("POST", "/api/v1/reservations/" + id + "/collect", requester, null, null, 403);
        request("POST", "/api/v1/reservations/" + id + "/collect", owner, null, null, 200);
        request("POST", "/api/v1/reservations/" + id + "/collect", owner, null, null, 200);
        request("POST", "/api/v1/reservations/" + id + "/cancel", requester, null, null, 409);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(1);
        assertThat(body(request("GET", "/api/v1/reservations/" + id + "/events", owner, null, null, 200)).size()).isEqualTo(2);
    }

    @Test void thirdPartiesCannotReadOrChangeReservations() throws Exception {
        UUID id = reserve(offer(2), 1);
        for (String suffix : List.of("", "/events")) request("GET", "/api/v1/reservations/" + id + suffix, other, null, null, 404);
        request("POST", "/api/v1/reservations/" + id + "/cancel", other, null, null, 404);
        request("POST", "/api/v1/reservations/" + id + "/cancel", owner, null, null, 403);
        assertThat(body(request("GET", "/api/v1/reservations", other, null, null, 200)).get("totalElements").asInt()).isZero();
        assertThat(body(request("GET", "/api/v1/reservations?status=RESERVED", owner, null, null, 200)).get("totalElements").asInt()).isEqualTo(1);
    }

    @Test void idempotentReplayDoesNotConsumeStockAndReturnsCurrentState() throws Exception {
        UUID offerId = offer(3), key = UUID.randomUUID();
        var payload = Map.of("offerId", offerId, "quantity", 1);
        var original = request("POST", "/api/v1/reservations", requester, payload, key, 201);
        var replay = request("POST", "/api/v1/reservations", requester, payload, key, 200);
        assertThat(body(replay).get("id")).isEqualTo(body(original).get("id"));
        assertThat(replay.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        UUID id = UUID.fromString(body(original).get("id").asText());
        reservations.cancel(id, requester);
        assertThat(body(request("POST", "/api/v1/reservations", requester, payload, key, 200)).get("status").asText()).isEqualTo("CANCELLED");
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(3);
    }

    @Test void keyCannotBeReusedForDifferentPayloadButIsScopedToAccount() throws Exception {
        UUID offerId = offer(5), key = UUID.randomUUID();
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", offerId, "quantity", 1), key, 201);
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", offerId, "quantity", 2), key, 409);
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", offer(2), "quantity", 1), key, 409);
        request("POST", "/api/v1/reservations", other, Map.of("offerId", offerId, "quantity", 1), key, 201);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(3);
    }

    @Test void invalidReservationsDoNotChangeStock() throws Exception {
        UUID offerId = offer(1);
        request("POST", "/api/v1/reservations", owner, Map.of("offerId", offerId, "quantity", 1), UUID.randomUUID(), 409);
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", offerId, "quantity", 2), UUID.randomUUID(), 409);
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", offerId, "quantity", 0), UUID.randomUUID(), 400);
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", UUID.randomUUID(), "quantity", 1), UUID.randomUUID(), 404);
        request("POST", "/api/v1/reservations", requester, Map.of("offerId", offerId, "quantity", 1), null, 400);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reservations", Long.class)).isZero();
    }

    @Test void cancelledReservationReleasesStockOnlyOnce() {
        UUID offerId = offer(2), id = reserve(offerId, 2);
        reservations.cancel(id, requester);
        reservations.cancel(id, requester);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(2);
        assertThat(reservations.history(id, requester)).hasSize(2);
        // O relógio está congelado: ordenar só pelo instante não define a sequência.
        assertThat(reservations.history(id, requester).stream().map(ReservationService.EventView::sequence))
                .containsExactly(0, 1);
        assertThat(reservations.history(id, requester).getFirst().status().name()).isEqualTo("RESERVED");
        assertThat(reservations.history(id, requester).getLast().status().name()).isEqualTo("CANCELLED");
    }

    @Test void archiveBlocksNewReservationsButPreservesExistingLifecycle() throws Exception {
        UUID offerId = offer(2), id = reserve(offerId, 1);
        request("POST", "/api/v1/offers/" + offerId + "/archive", requester, null, null, 403);
        request("POST", "/api/v1/offers/" + offerId + "/archive", owner, null, null, 200);
        request("POST", "/api/v1/reservations", other, Map.of("offerId", offerId, "quantity", 1), UUID.randomUUID(), 409);
        reservations.cancel(id, requester);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(2);
        assertThat(body(request("GET", "/api/v1/offers", null, null, null, 200)).get("totalElements").asInt()).isZero();
    }

    @Test void deadlineBlocksCollectionAndExpirationReturnsStockOnce() {
        UUID offerId = offer(2), id = reserve(offerId, 1);
        assertThat(reservations.due()).isEmpty();
        clock.set(clock.instant().plus(Duration.ofHours(25)));
        assertThatThrownBy(() -> reservations.collect(id, owner)).isInstanceOf(BusinessException.class);
        assertThat(reservations.due()).contains(id);
        assertThat(reservations.expireOne(id)).isTrue();
        assertThat(reservations.expireOne(id)).isFalse();
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(2);
        assertThat(reservations.history(id, owner)).hasSize(2);
        assertThat(reservations.history(id, owner).getLast().actorId()).isNull();
    }

    @Test void failedEventWriteRollsBackStockAndReservation() {
        UUID offerId = offer(2);
        doThrow(new IllegalStateException("simulated storage failure")).when(events).save(any(ReservationEvent.class));
        assertThatThrownBy(() -> reserve(offerId, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from reservations", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from reservation_events", Long.class)).isZero();
    }

    @Test void simultaneousRequestsCannotReserveTheLastUnitTwice() throws Exception {
        UUID offerId = offer(1);
        var results = concurrently(
                () -> outcome(() -> reservations.create(requester, UUID.randomUUID(), offerId, 1)),
                () -> outcome(() -> reservations.create(other, UUID.randomUUID(), offerId, 1)));
        assertThat(results).containsExactlyInAnyOrder("OK", "INSUFFICIENT_STOCK");
        assertThat(offers.get(offerId).availableQuantity()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from reservations", Long.class)).isEqualTo(1);
    }

    @Test void simultaneousIdenticalKeysCreateOneReservation() throws Exception {
        UUID offerId = offer(2), key = UUID.randomUUID();
        var results = concurrently(() -> reservations.create(requester, key, offerId, 1),
                () -> reservations.create(requester, key, offerId, 1));
        assertThat(results.getFirst().reservation().id()).isEqualTo(results.getLast().reservation().id());
        assertThat(results.stream().filter(ReservationService.CreationResult::replayed).count()).isEqualTo(1);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(1);
    }

    @Test void sameKeyRacingAcrossOffersCannotConsumeBothStocks() throws Exception {
        UUID first = offer(1), second = offer(1), key = UUID.randomUUID();
        var results = concurrently(
                () -> outcome(() -> reservations.create(requester, key, first, 1)),
                () -> outcome(() -> reservations.create(requester, key, second, 1)));
        assertThat(results).containsExactlyInAnyOrder("OK", "IDEMPOTENCY_KEY_REUSED");
        assertThat(offers.get(first).availableQuantity() + offers.get(second).availableQuantity()).isEqualTo(1);
    }

    @Test void simultaneousCancellationsAreBothSuccessfulAndReleaseOnce() throws Exception {
        UUID offerId = offer(2), id = reserve(offerId, 1);
        var results = concurrently(() -> outcome(() -> reservations.cancel(id, requester)),
                () -> outcome(() -> reservations.cancel(id, requester)));
        assertThat(results).containsExactly("OK", "OK");
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(2);
        assertThat(reservations.history(id, requester)).hasSize(2);
    }

    @Test void collectionAndCancellationRaceHasExactlyOneWinner() throws Exception {
        UUID offerId = offer(1), id = reserve(offerId, 1);
        var results = concurrently(() -> outcome(() -> reservations.collect(id, owner)),
                () -> outcome(() -> reservations.cancel(id, requester)));
        assertThat(results).containsExactlyInAnyOrder("OK", "INVALID_RESERVATION_STATE");
        var status = reservations.get(id, owner).status();
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(status.name().equals("CANCELLED") ? 1 : 0);
        assertThat(reservations.history(id, requester)).hasSize(2);
    }

    @Test void concurrentExpirationReleasesOnlyOnce() throws Exception {
        UUID offerId = offer(1), id = reserve(offerId, 1);
        clock.set(clock.instant().plus(Duration.ofHours(25)));
        assertThat(concurrently(() -> reservations.expireOne(id), () -> reservations.expireOne(id)))
                .containsExactlyInAnyOrder(true, false);
        assertThat(offers.get(offerId).availableQuantity()).isEqualTo(1);
        assertThat(reservations.history(id, requester)).hasSize(2);
    }

    @Test void swaggerAndHealthAreAvailableWithoutExposingSecrets() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        request("GET", "/actuator/env", owner, null, null, 403);
    }

    @Test void malformedBodyAndIdentifiersProduceClientErrors() throws Exception {
        request("GET", "/api/v1/offers/not-a-uuid", null, null, null, 400);
        mvc.perform(post("/api/v1/reservations").header("Authorization", "Bearer " + tokens.issue(requester).accessToken())
                .header("Idempotency-Key", "bad-key").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test void unsupportedMethodAndMediaTypeAreClientErrors() throws Exception {
        mvc.perform(put("/api/v1/offers").header("Authorization", "Bearer " + tokens.issue(owner).accessToken()))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(post("/api/v1/offers").header("Authorization", "Bearer " + tokens.issue(owner).accessToken())
                .contentType("text/plain").content("invalid"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    private String outcome(Runnable action) {
        try { action.run(); return "OK"; }
        catch (BusinessException exception) { return exception.code(); }
    }

    private <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<Future<T>>();
            for (var action : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                    return action.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.getFirst().get(20, TimeUnit.SECONDS), futures.getLast().get(20, TimeUnit.SECONDS));
        }
    }

    @TestConfiguration
    static class TimeConfig {
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>(Instant.now());
        void set(Instant instant) { current.set(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current.get(); }
    }
}
