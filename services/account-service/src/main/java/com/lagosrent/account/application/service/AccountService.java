package com.lagosrent.account.application.service;

import com.lagosrent.account.api.dto.*;
import com.lagosrent.account.application.exception.ResourceNotFoundException;
import com.lagosrent.account.domain.model.Account;
import com.lagosrent.account.domain.model.LandlordProfile;
import com.lagosrent.account.domain.model.SavedListing;
import com.lagosrent.account.domain.model.TenantProfile;
import com.lagosrent.account.infrastructure.persistence.AccountRepository;
import com.lagosrent.account.infrastructure.persistence.LandlordProfileRepository;
import com.lagosrent.account.infrastructure.persistence.SavedListingRepository;
import com.lagosrent.account.infrastructure.persistence.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final TenantProfileRepository tenantProfileRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final SavedListingRepository savedListingRepository;

    @Transactional
    public Account getOrCreateAccountFromJwt(Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        return accountRepository.findByKeycloakUserId(keycloakUserId).orElseGet(() -> {
            Account newAccount = new Account(
                    keycloakUserId,
                    email,
                    firstName,
                    lastName
            );
            return accountRepository.save(newAccount);
        });
    }

    @Transactional(readOnly = true)
    public Account getAccountByKeyCloakId(String keycloakUserId) {
        return accountRepository.findByKeycloakUserId(keycloakUserId).orElseThrow(() -> new ResourceNotFoundException("Account", keycloakUserId));
    }

    @Transactional
    public TenantProfileResponse updateTenantProfile(String keycloakUserId, UpdateTenantProfileRequest request) {
        Account account = getAccountByKeyCloakId(keycloakUserId);
        TenantProfile profile = tenantProfileRepository.findByAccountId(account.getId())
                .orElseGet(() -> new TenantProfile(account));

        profile.setOccupation(request.occupation());
        profile.setAnnualBudget(request.annualBudget());
        profile.setMoveInDate(request.moveInDate());
        profile.setPreferredLga(request.preferredLga());

        return TenantProfileResponse.from(tenantProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public TenantProfileResponse getTenantProfile(String keycloakUserId) {
        Account account = getAccountByKeyCloakId(keycloakUserId);
        TenantProfile profile = tenantProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TenantProfile for account", account.getId()));
        return TenantProfileResponse.from(profile);
    }

    @Transactional
    public LandlordProfileResponse updateLandlordProfile(String keycloakUserId, UpdateLandlordProfileRequest request) {
        Account account = getAccountByKeyCloakId(keycloakUserId);
        LandlordProfile profile = landlordProfileRepository.findByAccountId(account.getId())
                .orElseGet(() -> new LandlordProfile(account));

        profile.setBusinessName(request.businessName());
        profile.setBankAccountName(request.bankAccountName());
        profile.setBankAccountNumber(request.bankAccountNumber());
        profile.setBankName(request.bankName());

        return LandlordProfileResponse.from(landlordProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public LandlordProfileResponse getLandlordProfile(String keycloakUserid) {
        Account account = getAccountByKeyCloakId(keycloakUserid);
        LandlordProfile profile = landlordProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("LandlordProfile for account", account.getId()));
        return LandlordProfileResponse.from(profile);
    }

    @Transactional
    public void saveListing(String keycloakUserId, UUID listingId) {
        Account account = getAccountByKeyCloakId(keycloakUserId);
        if (savedListingRepository.findByAccountIdAndListingId(account.getId(), listingId).isEmpty()) {
            savedListingRepository.save(new SavedListing(account, listingId));
        }
    }

    @Transactional
    public void removedSavedListing(String keycloakUserId, UUID listingId) {
        Account account = getAccountByKeyCloakId(keycloakUserId);
        savedListingRepository.deleteByAccountIdAndListingId(account.getId(), listingId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getSavedListing(String keycloakUserId) {
        Account account = getAccountByKeyCloakId(keycloakUserId);
        return savedListingRepository.findByAccountId(account.getId())
                .stream().map(SavedListing::getId)
                .toList();
    }

    public UserProfileResponse getUserProfileByKeycloakId(String keycloakId) {
        Account account = getAccountByKeyCloakId(keycloakId);
        String firstName = account.getFirstName();
        String lastName = account.getLastName();
        String fullName = (firstName + " " + lastName).trim();

        return new UserProfileResponse(
                account.getKeycloakUserId(),
                account.getEmail(),
                fullName,
                account.getPhone()
        );
    }
}
