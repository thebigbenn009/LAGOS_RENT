package com.lagosrent.account.application.service;

import com.lagosrent.account.api.dto.AccountResponse;
import com.lagosrent.account.application.exception.ResourceNotFoundException;
import com.lagosrent.account.domain.model.Account;
import com.lagosrent.account.domain.model.AccountStatus;
import com.lagosrent.account.domain.model.AdminAuditRecord;
import com.lagosrent.account.infrastructure.persistence.AccountRepository;
import com.lagosrent.account.infrastructure.persistence.AdminAuditRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private final AccountRepository accountRepository;
    private final AdminAuditRecordRepository auditRecordRepository;

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccount(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::from);
    }

    @Transactional
    public AccountResponse suspendAccount(UUID accountId, String adminKeycloakId, String reason) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        String previousState = account.getStatus().name();
        account.setStatus(AccountStatus.SUSPENDED);

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "SUSPEND_ACCOUNT",
                "Account",
                accountId.toString(),
                reason,
                previousState,
                AccountStatus.SUSPENDED.name()
        ));

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse reactivateAccount(UUID accountId, String adminKeycloakId, String reason) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        String previousState = account.getStatus().name();
        account.setStatus(AccountStatus.ACTIVE);

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "REACTIVATE_ACCOUNT",
                "Account",
                accountId.toString(),
                reason,
                previousState,
                AccountStatus.ACTIVE.name()
        ));

        return AccountResponse.from(accountRepository.save(account));
    }
}
