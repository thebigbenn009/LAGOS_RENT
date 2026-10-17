package com.lagosrent.rental.application.service;

import com.lagosrent.rental.api.dto.AdminSagaResponse;
import com.lagosrent.rental.application.exception.ResourceNotFoundException;
import com.lagosrent.rental.domain.model.RentalSaga;
import com.lagosrent.rental.domain.model.SagaStatus;
import com.lagosrent.rental.infrastructure.persistence.RentalSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRentalService {
    private final RentalSagaRepository rentalSagaRepository;

    public Page<AdminSagaResponse> getSagas(SagaStatus status, Pageable pageable) {
        Page<RentalSaga> sagas = (status != null)
                ? rentalSagaRepository.findAllByStatus(status, pageable)
                : rentalSagaRepository.findAll(pageable);

        return sagas.map(AdminSagaResponse::from);
    }

    public AdminSagaResponse getSagaById(UUID sagaId) {
        return rentalSagaRepository.findById(sagaId)
                .map(AdminSagaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Saga  not found with id: " + sagaId));
    }
}
