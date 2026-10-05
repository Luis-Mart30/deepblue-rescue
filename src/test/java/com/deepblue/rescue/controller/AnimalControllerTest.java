package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    @Test
    void shouldFindAnimalByCode() throws Exception {
        AnimalResponse response = new AnimalResponse(
                1L,
                "ANM-001",
                "Luna",
                "Chelonia mydas",
                null,
                "CASE-001",
                null);

        when(animalService.findByCode("ANM-001"))
                .thenReturn(response);

        mockMvc.perform(get("/api/animals/ANM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.animalCode").value("ANM-001"))
                .andExpect(jsonPath("$.commonName").value("Luna"))
                .andExpect(jsonPath("$.scientificName")
                        .value("Chelonia mydas"))
                .andExpect(jsonPath("$.caseCode").value("CASE-001"));
    }

    @Test
    void shouldSendAnimalCodeToService() throws Exception {
        AnimalResponse response = new AnimalResponse(
                2L,
                "ANM-002",
                "Marina",
                "Delphinus delphis",
                null,
                "CASE-002",
                null);

        when(animalService.findByCode("ANM-002"))
                .thenReturn(response);

        mockMvc.perform(get("/api/animals/ANM-002"))
                .andExpect(status().isOk());

        verify(animalService).findByCode("ANM-002");
    }

    @Test
    void shouldFindAnimalsInRehabilitation() throws Exception {
        AnimalResponse animal = new AnimalResponse(
                1L,
                "ANM-001",
                "Luna",
                "Chelonia mydas",
                null,
                "CASE-001",
                null);

        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(animal));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].animalCode")
                        .value("ANM-001"))
                .andExpect(jsonPath("$[0].commonName")
                        .value("Luna"));
        verify(animalService)
                .findAnimalsInRehabilitation();

    }

    @Test
    void shouldReturnEmptyRehabilitationList() throws Exception {
        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldFindTreatmentsByAnimalCode() throws Exception {
        TreatmentResponse treatment = new TreatmentResponse(
                1L,
                "ANM-001",
                "SPC-001",
                LocalDateTime.of(2026, 10, 5, 10, 30),
                null,
                "Curación de heridas");

        when(treatmentService.findByAnimalCode("ANM-001"))
                .thenReturn(List.of(treatment));

        mockMvc.perform(get("/api/animals/ANM-001/treatments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].animalCode")
                        .value("ANM-001"))
                .andExpect(jsonPath("$[0].specialistCode")
                        .value("SPC-001"))
                .andExpect(jsonPath("$[0].description")
                        .value("Curación de heridas"));

        verify(treatmentService)
                .findByAnimalCode("ANM-001");
    }

    @Test
    void shouldReturnEmptyTreatmentList() throws Exception {
        when(treatmentService.findByAnimalCode("ANM-002"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/animals/ANM-002/treatments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnAnimalEligibleForTreatment() throws Exception {
        when(animalService.canReceiveTreatment("ANM-001"))
                .thenReturn(true);

        mockMvc.perform(
                get("/api/animals/ANM-001/treatment-eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode")
                        .value("ANM-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService)
                .canReceiveTreatment("ANM-001");
    }

    @Test
    void shouldReturnAnimalNotEligibleForTreatment() throws Exception {
        when(animalService.canReceiveTreatment("ANM-002"))
                .thenReturn(false);

        mockMvc.perform(
                get("/api/animals/ANM-002/treatment-eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode")
                        .value("ANM-002"))
                .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    void shouldReturnNotFoundWhenAnimalDoesNotExist() throws Exception {
        when(animalService.findByCode("ANM-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Animal not found: ANM-999"));

        mockMvc.perform(get("/api/animals/ANM-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Animal not found: ANM-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService).findByCode("ANM-999");
    }

    @Test
    void shouldReturnNotFoundWhenCheckingEligibilityOfMissingAnimal()
            throws Exception {

        when(animalService.canReceiveTreatment("ANM-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Animal not found: ANM-999"));

        mockMvc.perform(
                get("/api/animals/ANM-999/treatment-eligibility"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Animal not found: ANM-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService)
                .canReceiveTreatment("ANM-999");
    }
}