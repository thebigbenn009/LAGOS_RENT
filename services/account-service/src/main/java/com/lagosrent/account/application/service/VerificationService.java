package com.lagosrent.account.application.service;

import com.lagosrent.account.api.dto.SubmitVerificationCaseRequest;
import com.lagosrent.account.api.dto.VerificationCaseResponse;
import com.lagosrent.account.api.dto.VerificationStatusResponse;
import com.lagosrent.account.application.exception.ResourceNotFoundException;
import com.lagosrent.account.domain.model.*;
import com.lagosrent.account.infrastructure.messaging.payload.LandlordVerifiedPayload;
import com.lagosrent.account.infrastructure.persistence.AccountRepository;
import com.lagosrent.account.infrastructure.persistence.AdminAuditRecordRepository;
import com.lagosrent.account.infrastructure.persistence.LandlordProfileRepository;
import com.lagosrent.account.infrastructure.persistence.VerificationCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.lagosrent.account.domain.model.VerificationStatus.*;
import static com.lagosrent.account.infrastructure.messaging.config.RabbitMqConfig.ROUTING_KEY_LANDLORD_VERIFIED;
import static java.time.Instant.now;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final AccountRepository accountRepository;
    private final LandlordProfileRepository landlordProfileRepository;
    private final VerificationCaseRepository verificationCaseRepository;
    private final AdminAuditRecordRepository auditRecordRepository;
    private final OutboxService outboxService;

    @Transactional
    public VerificationCaseResponse submitVerificationCase(String keycloakUserId, SubmitVerificationCaseRequest request) {
        Account account = accountRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", keycloakUserId));

        LandlordProfile landlordProfile = landlordProfileRepository.findByAccountId(account.getId())
                .orElseGet(() -> landlordProfileRepository.save(new LandlordProfile(account)));

        VerificationCase verificationCase = new VerificationCase(
                landlordProfile,
                request.documentType(),
                request.documentS3Key(),
                request.documentFileName()
        );

        // Update profile status to PENDING if not already verified
        if (landlordProfile.getVerificationStatus() != VERIFIED) {
            landlordProfile.setVerificationStatus(PENDING);
            landlordProfileRepository.save(landlordProfile);
        }
        return VerificationCaseResponse.from(verificationCaseRepository.save(verificationCase));
    }

    @Transactional(readOnly = true)
    public VerificationStatusResponse getVerificationStatus(String keycloakUserId) {
        Account account = accountRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", keycloakUserId));

        LandlordProfile profile = landlordProfileRepository.findByAccountId(account.getId())
                .orElseGet(() -> new LandlordProfile(account));

        List<VerificationCaseResponse> cases = verificationCaseRepository.findByLandlordProfileId(profile.getId())
                .stream()
                .map(VerificationCaseResponse::from)
                .toList();

        return new VerificationStatusResponse(profile.getVerificationStatus(), cases);
    }

    @Transactional
    public VerificationCaseResponse approveVerificationCase(UUID caseId, String adminKeycloakId, String reason) {
        VerificationCase vc = verificationCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationCase", caseId));

        vc.setStatus(VerificationCaseStatus.APPROVED);
        vc.setReviewedAt(now());
        vc.setReviewedBy(adminKeycloakId);
        vc.setAdminNotes(reason);

        LandlordProfile profile = vc.getLandlordProfile();
        String previousState = profile.getVerificationStatus().name();
        profile.setVerificationStatus(VERIFIED);
        landlordProfileRepository.save(profile);

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "APPROVE_VERIFICATION_CASE",
                "LandlordProfile",
                profile.getId().toString(),
                reason,
                previousState,
                VERIFIED.name()
        ));
        // Atomically record outbox event within the same database transaction
        outboxService.recordEvent(
                "landlord",
                profile.getAccount().getKeycloakUserId(),
                1L,
                ROUTING_KEY_LANDLORD_VERIFIED,
                new LandlordVerifiedPayload(
                        profile.getAccount().getKeycloakUserId(),
                        profile.getId(),
                        vc.getDocumentType().name(),
                        adminKeycloakId,
                        now()
                ),
                null,
                null
        );
        return VerificationCaseResponse.from(verificationCaseRepository.save(vc));
    }

    @Transactional
    public VerificationCaseResponse rejectVerificationCase(UUID caseId, String adminKeycloakId, String reason) {
        VerificationCase vc = verificationCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationCase", caseId));

        vc.setStatus(VerificationCaseStatus.REJECTED);
        vc.setReviewedAt(now());
        vc.setReviewedBy(adminKeycloakId);
        vc.setAdminNotes(reason);

        LandlordProfile profile = vc.getLandlordProfile();
        String previousState = profile.getVerificationStatus().name();
        profile.setVerificationStatus(REJECTED);
        landlordProfileRepository.save(profile);

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "REJECT_VERIFICATION_CASE",
                "LandlordProfile",
                profile.getId().toString(),
                reason,
                previousState,
                REJECTED.name()
        ));

        return VerificationCaseResponse.from(verificationCaseRepository.save(vc));
    }
}
