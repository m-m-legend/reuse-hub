package io.github.mmlegend.reusehub.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, Clock clock,
                        @Value("${app.security.issuer}") String issuer,
                        @Value("${app.security.audience}") String audience,
                        @Value("${app.security.token-ttl}") Duration ttl) {
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("Token TTL deve estar entre 0 e 1 hora.");
        }
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.audience = audience;
        this.ttl = ttl;
    }

    public TokenResponse issue(UUID accountId) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(accountId.toString())
                .audience(List.of(audience)).issuedAt(now).expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
