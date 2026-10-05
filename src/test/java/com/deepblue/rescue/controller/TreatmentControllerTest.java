package com.deepblue.rescue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.service.TreatmentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TreatmentController.class)
@Import(GlobalExceptionHandler.class)
class TreatmentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private TreatmentService service;

        @Test
        void shouldRegisterTreatment() throws Exception {
                TreatmentResponse response = new TreatmentResponse(
                                1L,
                                "AN-001",
                                "SP-001",
                                LocalDateTime.of(2026, 10, 5, 9, 0),
                                TreatmentType.MEDICATION,
                                "Medication administered successfully");

                when(service.register(any(CreateTreatmentRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "AN-001",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2026-10-05T09:00:00",
                                                  "type": "MEDICATION",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                                .andExpect(jsonPath("$.specialistCode").value("SP-001"))
                                .andExpect(jsonPath("$.type").value("MEDICATION"))
                                .andExpect(jsonPath("$.description")
                                                .value("Medication administered successfully"));

                verify(service).register(any(CreateTreatmentRequest.class));
        }

        @Test
        void shouldReturnNotFoundWhenAnimalDoesNotExist() throws Exception {
                when(service.register(any(CreateTreatmentRequest.class)))
                                .thenThrow(new ResourceNotFoundException(
                                                "Animal not found: AN-999"));

                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "AN-999",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2026-10-05T09:00:00",
                                                  "type": "MEDICATION",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message")
                                                .value("Animal not found: AN-999"))
                                .andExpect(jsonPath("$.details").isEmpty());

                verify(service).register(any(CreateTreatmentRequest.class));
        }

        @Test
        void shouldReturnBadRequestWhenAnimalCodeIsBlank() throws Exception {
                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2026-10-05T09:00:00",
                                                  "type": "MEDICATION",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.message")
                                                .value("Request validation failed"))
                                .andExpect(jsonPath("$.details.animalCode")
                                                .value("Animal code is required"));

                verify(service, never())
                                .register(any(CreateTreatmentRequest.class));
        }

        @Test
        void shouldReturnBadRequestWhenDateIsInFuture() throws Exception {
                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "AN-001",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2099-10-05T09:00:00",
                                                  "type": "MEDICATION",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.message")
                                                .value("Request validation failed"))
                                .andExpect(jsonPath("$.details.performedAt")
                                                .value("Treatment date cannot be in the future"));

                verify(service, never())
                                .register(any(CreateTreatmentRequest.class));
        }

        @Test
        void shouldReturnBadRequestWhenTreatmentTypeIsInvalid() throws Exception {
                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "AN-001",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2026-10-05T09:00:00",
                                                  "type": "INVALID_TYPE",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.message")
                                                .value("Malformed or invalid JSON request"))
                                .andExpect(jsonPath("$.details.body")
                                                .value("Check JSON syntax and enum values"));

                verify(service, never())
                                .register(any(CreateTreatmentRequest.class));
        }

        @Test
        void shouldReturnConflictWhenBusinessRuleIsViolated() throws Exception {
                when(service.register(any(CreateTreatmentRequest.class)))
                                .thenThrow(new BusinessRuleException(
                                                "Treatment cannot be registered"));

                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "AN-001",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2026-10-05T09:00:00",
                                                  "type": "MEDICATION",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"))
                                .andExpect(jsonPath("$.message")
                                                .value("Treatment cannot be registered"))
                                .andExpect(jsonPath("$.details").isEmpty());

                verify(service).register(any(CreateTreatmentRequest.class));
        }

        @Test
        void shouldReturnInternalServerErrorWhenUnexpectedErrorOccurs()
                        throws Exception {

                when(service.register(any(CreateTreatmentRequest.class)))
                                .thenThrow(new RuntimeException("Unexpected database error"));

                mockMvc.perform(post("/api/treatments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "animalCode": "AN-001",
                                                  "specialistCode": "SP-001",
                                                  "performedAt": "2026-10-05T09:00:00",
                                                  "type": "MEDICATION",
                                                  "description": "Medication administered successfully"
                                                }
                                                """))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.status").value(500))
                                .andExpect(jsonPath("$.error")
                                                .value("Internal Server Error"))
                                .andExpect(jsonPath("$.message")
                                                .value("An unexpected error occurred"))
                                .andExpect(jsonPath("$.details").isEmpty());

                verify(service).register(any(CreateTreatmentRequest.class));
        }

}