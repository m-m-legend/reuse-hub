package io.github.mmlegend.reusehub.identity.application;

import io.github.mmlegend.reusehub.identity.domain.Account;
import io.github.mmlegend.reusehub.identity.persistence.AccountRepository;
import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(AccountRepository accounts, PasswordEncoder passwords, TokenService tokens, Clock clock) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
        this.clock = clock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AccountView register(String name, String email, String password) {
        checkPasswordLength(password);
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmail(normalized)) {
            throw BusinessException.conflict("EMAIL_IN_USE", "Este e-mail já está cadastrado.");
        }
        Account account = new Account(name.strip(), normalized, passwords.encode(password), clock.instant());
        return AccountView.from(accounts.saveAndFlush(account));
    }

    @Transactional(readOnly = true)
    public TokenService.TokenResponse login(String email, String password) {
        checkPasswordLength(password);
        var account = accounts.findByEmail(email.strip().toLowerCase(Locale.ROOT));
        boolean matches = passwords.matches(password, account.map(Account::getPasswordHash).orElse(dummyHash));
        if (account.isEmpty() || !matches) {
            throw new BusinessException(BusinessException.Kind.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
        }
        return tokens.issue(account.get().getId());
    }

    @Transactional(readOnly = true)
    public AccountView me(UUID id) {
        return AccountView.from(accounts.findById(id).orElseThrow(() -> BusinessException.notFound("Conta")));
    }

    private void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(BusinessException.Kind.INVALID, "PASSWORD_TOO_LONG", "A senha deve ter no máximo 72 bytes UTF-8.");
        }
    }

    public record AccountView(UUID id, String name, String email, Instant createdAt) {
        static AccountView from(Account account) {
            return new AccountView(account.getId(), account.getName(), account.getEmail(), account.getCreatedAt());
        }
    }
}
