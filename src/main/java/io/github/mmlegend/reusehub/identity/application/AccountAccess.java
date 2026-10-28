package io.github.mmlegend.reusehub.identity.application;

import io.github.mmlegend.reusehub.identity.persistence.AccountRepository;
import io.github.mmlegend.reusehub.shared.domain.BusinessException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Porta pública do módulo identity para serializar pedidos do mesmo usuário. */
@Service
public class AccountAccess {
    private final AccountRepository accounts;

    public AccountAccess(AccountRepository accounts) { this.accounts = accounts; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(UUID id) {
        accounts.findLockedById(id).orElseThrow(() -> BusinessException.notFound("Conta"));
    }
}
