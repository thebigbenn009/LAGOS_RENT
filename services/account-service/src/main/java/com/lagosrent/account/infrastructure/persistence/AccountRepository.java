package com.lagosrent.account.infrastructure.persistence;


import com.lagosrent.account.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByKeycloakUserId(String keycloakUserId);

    Optional<Account> findByEmail(String email);

    boolean existsByKeycloakUserId(String keycloakUserId);
}
