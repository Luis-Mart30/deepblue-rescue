package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.RescueCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RescueCaseServiceImpl implements RescueCaseService {

    private final RescueCaseRepository rescueCaseRepository;
    private final RescueCaseMapper rescueCaseMapper;

    public RescueCaseServiceImpl(
            RescueCaseRepository rescueCaseRepository,
            RescueCaseMapper rescueCaseMapper
    ) {
        this.rescueCaseRepository = rescueCaseRepository;
        this.rescueCaseMapper = rescueCaseMapper;
    }

    @Override
    public RescueCaseResponse findByCode(String caseCode) {
        return rescueCaseRepository.findByCaseCode(caseCode)
                .map(rescueCaseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rescue case not found: " + caseCode
                ));
    }

    @Override
    public List<RescueCaseResponse> findByStatus(RescueStatus status) {
        return rescueCaseRepository.findByStatusOrderByRescueDateAsc(status)
                .stream()
                .map(rescueCaseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RescueCaseResponse changeStatus(
            String caseCode,
            ChangeRescueStatusRequest request
    ) {
        RescueCase rescueCase = rescueCaseRepository.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rescue case not found: " + caseCode
                ));

        RescueStatus currentStatus = rescueCase.getStatus();
        RescueStatus nextStatus = request.status();

        if (!isValidTransition(currentStatus, nextStatus)) {
            throw new BusinessRuleException(
                    "Invalid status transition from "
                            + currentStatus + " to " + nextStatus
            );
        }

        rescueCase.setStatus(nextStatus);
        RescueCase savedCase = rescueCaseRepository.save(rescueCase);

        return rescueCaseMapper.toResponse(savedCase);
    }

    private boolean isValidTransition(
            RescueStatus currentStatus,
            RescueStatus nextStatus
    ) {
        return switch (currentStatus) {
            case ADMITTED -> nextStatus == RescueStatus.UNDER_EVALUATION;
            case UNDER_EVALUATION -> nextStatus == RescueStatus.IN_REHABILITATION;
            case IN_REHABILITATION -> nextStatus == RescueStatus.READY_FOR_RELEASE;
            case READY_FOR_RELEASE -> nextStatus == RescueStatus.RELEASED;
            default -> false;
        };
    }
}
