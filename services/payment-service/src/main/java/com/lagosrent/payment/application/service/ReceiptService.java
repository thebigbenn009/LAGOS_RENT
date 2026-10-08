package com.lagosrent.payment.application.service;

import com.lagosrent.payment.api.dto.ReceiptResponse;
import com.lagosrent.payment.application.exception.ResourceNotFoundException;
import com.lagosrent.payment.infrastructure.persistence.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptRepository receiptRepository;

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByPayableId(UUID payableId) {
        return receiptRepository.findByPayableId(payableId)
                .map(ReceiptResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt for Payable", payableId));
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> getTenantReceipts(String tenantKeycloakId) {
        return receiptRepository.findByIssuedToTenantKeycloakId(tenantKeycloakId).stream()
                .map(ReceiptResponse::from)
                .toList();
    }

}
