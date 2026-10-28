package io.github.mmlegend.reusehub.identity.api;

import io.github.mmlegend.reusehub.identity.application.AuthService;
import io.github.mmlegend.reusehub.identity.application.TokenService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AccountView register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request.name(), request.email(), request.password());
    }

    @PostMapping("/login")
    public TokenService.TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public AuthService.AccountView me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(UUID.fromString(jwt.getSubject()));
    }

    public record RegisterRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 12, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {}
}
