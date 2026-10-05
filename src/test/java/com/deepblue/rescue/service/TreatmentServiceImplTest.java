package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper treatmentMapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    void shouldRegisterValidTreatment() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        Specialist specialist = activeSpecialist(true);
        LocalDateTime performedAt = LocalDateTime.of(2026, 8, 21, 9, 0);
        CreateTreatmentRequest request = requestAt(performedAt);
        TreatmentResponse response = new TreatmentResponse(
                1L,
                "AN-001",
                "SPEC-001",
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(treatmentMapper.toResponse(any(Treatment.class)))
                .thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(captor.capture());
        Treatment savedTreatment = captor.getValue();

        assertThat(savedTreatment.getAnimal()).isSameAs(animal);
        assertThat(savedTreatment.getSpecialist()).isSameAs(specialist);
        assertThat(savedTreatment.getPerformedAt()).isEqualTo(performedAt);
        assertThat(savedTreatment.getType()).isEqualTo(TreatmentType.WOUND_CARE);
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        CreateTreatmentRequest request = requestAt(
                LocalDateTime.of(2026, 8, 21, 9, 0)
        );
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-001");

        verify(specialistRepository, never()).findByProfessionalCode(any());
        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSpecialistDoesNotExist() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        CreateTreatmentRequest request = requestAt(
                LocalDateTime.of(2026, 8, 21, 9, 0)
        );
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SPEC-001");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectInactiveSpecialistWithoutSaving() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        Specialist specialist = activeSpecialist(false);
        CreateTreatmentRequest request = requestAt(
                LocalDateTime.of(2026, 8, 21, 9, 0)
        );
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");

        verify(treatmentRepository, never()).save(any());
        verify(treatmentMapper, never()).toResponse(any());
    }

    @Test
    void shouldRejectTreatmentForReleasedAnimalWithoutSaving() {
        Animal animal = animalWithCaseStatus(RescueStatus.RELEASED);
        Specialist specialist = activeSpecialist(true);
        CreateTreatmentRequest request = requestAt(
                LocalDateTime.of(2026, 8, 21, 9, 0)
        );
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RELEASED");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDateWithoutSaving() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        Specialist specialist = activeSpecialist(true);
        CreateTreatmentRequest request = requestAt(
                LocalDateTime.of(2026, 8, 19, 10, 0)
        );
        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("before the rescue date");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldFindTreatmentsByAnimalCodeInRepositoryOrder() {
        Animal animal = animalWithCaseStatus(RescueStatus.IN_REHABILITATION);
        Specialist specialist = activeSpecialist(true);
        LocalDateTime firstDate = LocalDateTime.of(2026, 8, 21, 9, 0);
        LocalDateTime secondDate = LocalDateTime.of(2026, 8, 22, 11, 0);
        Treatment firstTreatment = new Treatment(
                animal, specialist, firstDate,
                TreatmentType.WOUND_CARE, "Initial cleaning"
        );
        Treatment secondTreatment = new Treatment(
                animal, specialist, secondDate,
                TreatmentType.HYDRATION, "Hydration control"
        );
        TreatmentResponse firstResponse = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", firstDate,
                TreatmentType.WOUND_CARE, "Initial cleaning"
        );
        TreatmentResponse secondResponse = new TreatmentResponse(
                2L, "AN-001", "SPEC-001", secondDate,
                TreatmentType.HYDRATION, "Hydration control"
        );

        when(treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001"))
                .thenReturn(List.of(firstTreatment, secondTreatment));
        when(treatmentMapper.toResponse(firstTreatment)).thenReturn(firstResponse);
        when(treatmentMapper.toResponse(secondTreatment)).thenReturn(secondResponse);

        List<TreatmentResponse> result = service.findByAnimalCode("AN-001");

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    private Animal animalWithCaseStatus(RescueStatus status) {
        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                status
        );
        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private Specialist activeSpecialist(boolean active) {
        return new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena.vargas@deepblue.org",
                active
        );
    }

    private CreateTreatmentRequest requestAt(LocalDateTime performedAt) {
        return new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );
    }
}
