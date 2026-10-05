package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository rescueCaseRepository;

    @Mock
    private RescueCaseMapper rescueCaseMapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
    void shouldFindRescueCaseByCode() {
        LocalDate rescueDate = LocalDate.of(2026, 8, 20);
        RescueCase rescueCase = new RescueCase(
                "RES-001",
                rescueDate,
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION
        );
        RescueCaseResponse response = new RescueCaseResponse(
                1L,
                "RES-001",
                rescueDate,
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION,
                "CENTER-001",
                "AN-001"
        );

        when(rescueCaseRepository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(rescueCaseMapper.toResponse(rescueCase))
                .thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);
        verify(rescueCaseRepository).findByCaseCode("RES-001");
        verify(rescueCaseMapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowWhenRescueCaseDoesNotExist() {
        when(rescueCaseRepository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(rescueCaseMapper, never()).toResponse(any());
    }

    @Test
    void shouldFindRescueCasesByStatus() {
        LocalDate firstDate = LocalDate.of(2026, 8, 20);
        LocalDate secondDate = LocalDate.of(2026, 8, 21);
        RescueCase firstCase = new RescueCase(
                "RES-010", firstDate, "Taganga", RescueStatus.ADMITTED
        );
        RescueCase secondCase = new RescueCase(
                "RES-011", secondDate, "Rodadero", RescueStatus.ADMITTED
        );
        RescueCaseResponse firstResponse = new RescueCaseResponse(
                10L, "RES-010", firstDate, "Taganga",
                RescueStatus.ADMITTED, "CENTER-001", null
        );
        RescueCaseResponse secondResponse = new RescueCaseResponse(
                11L, "RES-011", secondDate, "Rodadero",
                RescueStatus.ADMITTED, "CENTER-001", null
        );

        when(rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                RescueStatus.ADMITTED
        )).thenReturn(List.of(firstCase, secondCase));
        when(rescueCaseMapper.toResponse(firstCase)).thenReturn(firstResponse);
        when(rescueCaseMapper.toResponse(secondCase)).thenReturn(secondResponse);

        List<RescueCaseResponse> result = service.findByStatus(
                RescueStatus.ADMITTED
        );

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        LocalDate rescueDate = LocalDate.of(2026, 8, 20);
        RescueCase rescueCase = new RescueCase(
                "RES-002",
                rescueDate,
                "Taganga",
                RescueStatus.ADMITTED
        );
        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(
                RescueStatus.UNDER_EVALUATION
        );
        RescueCaseResponse response = new RescueCaseResponse(
                2L,
                "RES-002",
                rescueDate,
                "Taganga",
                RescueStatus.UNDER_EVALUATION,
                "CENTER-001",
                null
        );

        when(rescueCaseRepository.findByCaseCode("RES-002"))
                .thenReturn(Optional.of(rescueCase));
        when(rescueCaseRepository.save(rescueCase)).thenReturn(rescueCase);
        when(rescueCaseMapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus("RES-002", request);

        assertThat(result).isEqualTo(response);
        assertThat(rescueCase.getStatus())
                .isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(rescueCaseRepository).save(rescueCase);
    }

    @Test
    void shouldRejectInvalidStatusTransitionWithoutSaving() {
        RescueCase rescueCase = new RescueCase(
                "RES-003",
                LocalDate.of(2026, 8, 20),
                "Taganga",
                RescueStatus.ADMITTED
        );
        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(
                RescueStatus.READY_FOR_RELEASE
        );

        when(rescueCaseRepository.findByCaseCode("RES-003"))
                .thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus("RES-003", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ADMITTED")
                .hasMessageContaining("READY_FOR_RELEASE");

        verify(rescueCaseRepository, never()).save(any());
        verify(rescueCaseMapper, never()).toResponse(any());
    }
}
