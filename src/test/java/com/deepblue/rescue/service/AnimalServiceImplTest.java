package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
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
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalMapper animalMapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldFindAnimalByCode() {
        Animal animal = animalWithStatus(
                "AN-100", RescueStatus.IN_REHABILITATION
        );
        AnimalResponse response = responseFor(
                "AN-100", RescueStatus.IN_REHABILITATION
        );
        when(animalRepository.findByAnimalCode("AN-100"))
                .thenReturn(Optional.of(animal));
        when(animalMapper.toResponse(animal)).thenReturn(response);

        AnimalResponse result = service.findByCode("AN-100");

        assertThat(result).isEqualTo(response);
        verify(animalMapper).toResponse(animal);
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");

        verify(animalMapper, never()).toResponse(any());
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        Animal firstAnimal = animalWithStatus(
                "AN-101", RescueStatus.IN_REHABILITATION
        );
        Animal secondAnimal = animalWithStatus(
                "AN-102", RescueStatus.IN_REHABILITATION
        );
        AnimalResponse firstResponse = responseFor(
                "AN-101", RescueStatus.IN_REHABILITATION
        );
        AnimalResponse secondResponse = responseFor(
                "AN-102", RescueStatus.IN_REHABILITATION
        );
        when(animalRepository.findByRescueCaseStatus(
                RescueStatus.IN_REHABILITATION
        )).thenReturn(List.of(firstAnimal, secondAnimal));
        when(animalMapper.toResponse(firstAnimal)).thenReturn(firstResponse);
        when(animalMapper.toResponse(secondAnimal)).thenReturn(secondResponse);

        List<AnimalResponse> result = service.findAnimalsInRehabilitation();

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void shouldAllowTreatmentWhileAnimalIsUnderEvaluation() {
        Animal animal = animalWithStatus(
                "AN-103", RescueStatus.UNDER_EVALUATION
        );
        when(animalRepository.findByAnimalCode("AN-103"))
                .thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-103")).isTrue();
    }

    @Test
    void shouldAllowTreatmentWhileAnimalIsInRehabilitation() {
        Animal animal = animalWithStatus(
                "AN-104", RescueStatus.IN_REHABILITATION
        );
        when(animalRepository.findByAnimalCode("AN-104"))
                .thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-104")).isTrue();
    }

    @Test
    void shouldRejectTreatmentForReleasedAnimal() {
        Animal animal = animalWithStatus(
                "AN-105", RescueStatus.RELEASED
        );
        when(animalRepository.findByAnimalCode("AN-105"))
                .thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-105")).isFalse();
    }

    private Animal animalWithStatus(String animalCode, RescueStatus status) {
        RescueCase rescueCase = new RescueCase(
                "RES-" + animalCode.substring(3),
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                status
        );
        Animal animal = new Animal(
                animalCode,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private AnimalResponse responseFor(
            String animalCode,
            RescueStatus status
    ) {
        return new AnimalResponse(
                1L,
                animalCode,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                "RES-" + animalCode.substring(3),
                status
        );
    }
}
