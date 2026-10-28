package io.github.mmlegend.reusehub.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecretKeySpec signingKey(@Value("${app.security.secret}") String secret) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT_SECRET deve ser Base64 válido.");
        }
        if (decoded.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes aleatórios em Base64.");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKeySpec key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKeySpec key, @Value("${app.security.issuer}") String issuer,
                          @Value("${app.security.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                if (jwt.getExpiresAt() != null && jwt.getAudience().contains(audience)) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // Subject precisa identificar uma conta por UUID.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), claims));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/offers", "/api/v1/offers/*").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> {})
                        .authenticationEntryPoint((request, response, exception) ->
                                problem(json, request, response, 401, "UNAUTHORIZED")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                problem(json, request, response, 401, "UNAUTHORIZED"))
                        .accessDeniedHandler((request, response, exception) ->
                                problem(json, request, response, 403, "FORBIDDEN")))
                .build();
    }

    private void problem(ObjectMapper json, HttpServletRequest request, HttpServletResponse response,
                         int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        json.writeValue(response.getOutputStream(), Map.of(
                "type", "urn:reuse-hub:problem:" + code.toLowerCase(java.util.Locale.ROOT),
                "title", code, "status", status, "code", code,
                "detail", status == 401 ? "Autenticação necessária ou token inválido." : "Acesso negado.",
                "instance", request.getRequestURI(), "requestId", String.valueOf(request.getAttribute("requestId"))));
    }
}
