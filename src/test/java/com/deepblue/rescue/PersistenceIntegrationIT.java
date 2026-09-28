package com.deepblue.rescue;

import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;

import java.time.LocalDate;
import java.util.List;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.MedicalRecord;

import java.math.BigDecimal;

import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.Specialist;

import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldExecuteV1V2AndV3() {
        Integer migrations = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = true
                  AND version IN ('1', '2' ,'3')
                """,
                Integer.class
        );

        assertThat(migrations).isEqualTo(3);
    }

    @Test
void inheritedRepositoryMethodsShouldWork() {
    long initialCount = rescueCenterRepository.count();

    RescueCenter center = new RescueCenter(
            "DB-CAR",
            "DeepBlue Caribbean Center",
            "Santa Marta"
    );

    RescueCenter savedCenter =
            rescueCenterRepository.save(center);

    assertThat(savedCenter.getId()).isNotNull();

    assertThat(
            rescueCenterRepository.findById(savedCenter.getId())
    ).contains(savedCenter);

    assertThat(
            rescueCenterRepository.existsById(savedCenter.getId())
    ).isTrue();

    assertThat(
            rescueCenterRepository.count()
    ).isEqualTo(initialCount + 1);
}

@Test
void oneCenterShouldHaveManyRescueCases() {
    RescueCenter center = new RescueCenter(
            "DB-REL",
            "DeepBlue Relationship Center",
            "Santa Marta"
    );

    RescueCase firstCase = new RescueCase(
            "RES-REL-001",
            LocalDate.of(2026, 8, 1),
            "Taganga",
            RescueStatus.ADMITTED
    );

    RescueCase secondCase = new RescueCase(
            "RES-REL-002",
            LocalDate.of(2026, 8, 2),
            "Bahía Concha",
            RescueStatus.UNDER_EVALUATION
    );

    center.addCase(firstCase);
    center.addCase(secondCase);

    rescueCenterRepository.saveAndFlush(center);

    List<RescueCase> cases =
            rescueCaseRepository.findByRescueCenterCode("DB-REL");

    assertThat(cases).hasSize(2);

    assertThat(cases)
            .allSatisfy(rescueCase ->
                    assertThat(
                            rescueCase.getRescueCenter().getCode()
                    ).isEqualTo("DB-REL")
            );
}

@Test
void rescueCaseShouldHaveOneAnimal() {
    RescueCenter center = new RescueCenter(
            "DB-ONE",
            "DeepBlue One-to-One Center",
            "Santa Marta"
    );

    RescueCase rescueCase = new RescueCase(
            "RES-2026-001",
            LocalDate.of(2026, 8, 5),
            "Bahía Concha",
            RescueStatus.ADMITTED
    );

    Animal animal = new Animal(
            "AN-2026-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
    );

    center.addCase(rescueCase);
    rescueCase.assignAnimal(animal);

    rescueCenterRepository.saveAndFlush(center);

    RescueCase savedCase = rescueCaseRepository
            .findByCaseCode("RES-2026-001")
            .orElseThrow();

    Animal savedAnimal = animalRepository
            .findByAnimalCode("AN-2026-001")
            .orElseThrow();

    assertThat(savedCase.getAnimal()).isNotNull();
    assertThat(savedCase.getAnimal().getAnimalCode())
            .isEqualTo("AN-2026-001");

    assertThat(savedAnimal.getRescueCase()).isNotNull();
    assertThat(savedAnimal.getRescueCase().getCaseCode())
            .isEqualTo("RES-2026-001");
}

@Test
void animalShouldHaveOneMedicalRecord() {
    RescueCenter center = new RescueCenter(
            "DB-MED",
            "DeepBlue Medical Center",
            "Santa Marta"
    );

    RescueCase rescueCase = new RescueCase(
            "RES-2026-002",
            LocalDate.of(2026, 8, 6),
            "Playa Grande",
            RescueStatus.UNDER_EVALUATION
    );

    Animal animal = new Animal(
            "AN-2026-002",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
    );

    MedicalRecord medicalRecord = new MedicalRecord(
            new BigDecimal("28.40"),
            "STABLE",
            "Left front flipper injury",
            null
    );

    center.addCase(rescueCase);
    rescueCase.assignAnimal(animal);
    animal.assignMedicalRecord(medicalRecord);

    rescueCenterRepository.saveAndFlush(center);

    assertThat(animal.getId()).isNotNull();
    assertThat(medicalRecord.getId()).isNotNull();
    assertThat(animal.getMedicalRecord()).isSameAs(medicalRecord);
    assertThat(medicalRecord.getAnimal()).isSameAs(animal);
}

@Test
void specialistShouldHaveManyExpertiseAreas() {
    Expertise trauma = expertiseRepository
            .findByNameIgnoreCase("Trauma")
            .orElseThrow();

    Expertise rehabilitation = expertiseRepository
            .findByNameIgnoreCase("Rehabilitation")
            .orElseThrow();

    Specialist elena = new Specialist(
            "SPEC-NM-001",
            "Elena",
            "Vargas",
            "elena.nm@deepblue.org",
            true
    );

    elena.addExpertise(trauma);
    elena.addExpertise(rehabilitation);

    Specialist savedSpecialist =
            specialistRepository.saveAndFlush(elena);

    assertThat(savedSpecialist.getId()).isNotNull();
    assertThat(savedSpecialist.getExpertiseAreas()).hasSize(2);

    assertThat(savedSpecialist.getExpertiseAreas())
            .extracting(Expertise::getName)
            .containsExactlyInAnyOrder(
                    "Trauma",
                    "Rehabilitation"
            );
}

@Test
void shouldFindCasesByStatusOrderedByRescueDate() {
    RescueCenter center = new RescueCenter(
            "DB-STATUS",
            "DeepBlue Status Center",
            "Santa Marta"
    );

    RescueCase firstCase = new RescueCase(
            "RES-001",
            LocalDate.of(2026, 8, 3),
            "Taganga",
            RescueStatus.IN_REHABILITATION
    );

    RescueCase secondCase = new RescueCase(
            "RES-002",
            LocalDate.of(2026, 8, 5),
            "Playa Grande",
            RescueStatus.READY_FOR_RELEASE
    );

    RescueCase thirdCase = new RescueCase(
            "RES-003",
            LocalDate.of(2026, 8, 7),
            "Bahía Concha",
            RescueStatus.IN_REHABILITATION
    );

    center.addCase(firstCase);
    center.addCase(secondCase);
    center.addCase(thirdCase);

    rescueCenterRepository.saveAndFlush(center);

    List<RescueCase> results =
            rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                    RescueStatus.IN_REHABILITATION
            );

    assertThat(results).hasSize(2);

    assertThat(results)
            .extracting(RescueCase::getCaseCode)
            .containsExactly("RES-001", "RES-003");
}

@Test
void shouldFindAnimalsBelongingToSpecificCenter() {
    RescueCenter caribbeanCenter = new RescueCenter(
            "DB-CAR",
            "DeepBlue Caribbean",
            "Santa Marta"
    );

    RescueCase caribbeanCase = new RescueCase(
            "RES-CAR-001",
            LocalDate.of(2026, 8, 10),
            "Bahía Concha",
            RescueStatus.IN_REHABILITATION
    );

    Animal caribbeanAnimal = new Animal(
            "AN-CAR-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
    );

    caribbeanCenter.addCase(caribbeanCase);
    caribbeanCase.assignAnimal(caribbeanAnimal);

    RescueCenter pacificCenter = new RescueCenter(
            "DB-PAC",
            "DeepBlue Pacific",
            "Buenaventura"
    );

    RescueCase pacificCase = new RescueCase(
            "RES-PAC-001",
            LocalDate.of(2026, 8, 11),
            "Bahía Málaga",
            RescueStatus.ADMITTED
    );

    Animal pacificAnimal = new Animal(
            "AN-PAC-001",
            "Humpback Whale",
            "Megaptera novaeangliae",
            AnimalSex.UNKNOWN
    );

    pacificCenter.addCase(pacificCase);
    pacificCase.assignAnimal(pacificAnimal);

    rescueCenterRepository.saveAllAndFlush(
            List.of(caribbeanCenter, pacificCenter)
    );

    List<Animal> results =
            animalRepository.findByRescueCaseRescueCenterCode(
                    "DB-CAR"
            );

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getAnimalCode())
            .isEqualTo("AN-CAR-001");

    assertThat(results)
            .allSatisfy(animal ->
                    assertThat(
                            animal.getRescueCase()
                                    .getRescueCenter()
                                    .getCode()
                    ).isEqualTo("DB-CAR")
            );
}

@Test
void shouldFindActiveSpecialistsByExpertiseUsingJpql() {
    Expertise trauma = expertiseRepository
            .findByNameIgnoreCase("Trauma")
            .orElseThrow();

    Expertise rehabilitation = expertiseRepository
            .findByNameIgnoreCase("Rehabilitation")
            .orElseThrow();

    Expertise marineMammals = expertiseRepository
            .findByNameIgnoreCase("Marine Mammals")
            .orElseThrow();

    Expertise marineBirds = expertiseRepository
            .findByNameIgnoreCase("Marine Birds")
            .orElseThrow();

    Specialist elena = new Specialist(
            "SPEC-JPQL-001",
            "Elena",
            "Vargas",
            "elena.jpql@deepblue.org",
            true
    );
    elena.addExpertise(trauma);
    elena.addExpertise(rehabilitation);

    Specialist mateo = new Specialist(
            "SPEC-JPQL-002",
            "Mateo",
            "Rojas",
            "mateo.jpql@deepblue.org",
            true
    );
    mateo.addExpertise(marineMammals);
    mateo.addExpertise(rehabilitation);

    Specialist sofia = new Specialist(
            "SPEC-JPQL-003",
            "Sofia",
            "Torres",
            "sofia.jpql@deepblue.org",
            true
    );
    sofia.addExpertise(marineBirds);
    sofia.addExpertise(trauma);

    specialistRepository.saveAllAndFlush(
            List.of(elena, mateo, sofia)
    );

    List<Specialist> results =
            specialistRepository.findActiveByExpertise("trauma");

    assertThat(results).hasSize(2);

    assertThat(results)
            .extracting(Specialist::getFirstName)
            .containsExactlyInAnyOrder("Elena", "Sofia");
}

@Test
void shouldFindAnimalTreatmentsInChronologicalOrder() {
    RescueCenter center = new RescueCenter(
            "DB-TRT",
            "DeepBlue Treatment Center",
            "Santa Marta"
    );

    RescueCase rescueCase = new RescueCase(
            "RES-TRT-001",
            LocalDate.of(2026, 8, 1),
            "Taganga",
            RescueStatus.IN_REHABILITATION
    );

    Animal animal = new Animal(
            "AN-TRT-001",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
    );

    center.addCase(rescueCase);
    rescueCase.assignAnimal(animal);
    rescueCenterRepository.saveAndFlush(center);

    Specialist elena = new Specialist(
            "SPEC-TRT-001",
            "Elena",
            "Vargas",
            "elena.treatment@deepblue.org",
            true
    );

    Specialist mateo = new Specialist(
            "SPEC-TRT-002",
            "Mateo",
            "Rojas",
            "mateo.treatment@deepblue.org",
            true
    );

    specialistRepository.saveAllAndFlush(List.of(elena, mateo));

    Treatment treatment1 = new Treatment(
            animal,
            elena,
            LocalDateTime.of(2026, 8, 2, 9, 0),
            TreatmentType.WOUND_CARE,
            "Cleaning of left front flipper"
    );

    Treatment treatment2 = new Treatment(
            animal,
            elena,
            LocalDateTime.of(2026, 8, 3, 10, 0),
            TreatmentType.HYDRATION,
            "Subcutaneous fluid therapy"
    );

    Treatment treatment3 = new Treatment(
            animal,
            mateo,
            LocalDateTime.of(2026, 8, 4, 11, 0),
            TreatmentType.OBSERVATION,
            "General observation"
    );

    treatmentRepository.saveAllAndFlush(
            List.of(treatment3, treatment1, treatment2)
    );

    List<Treatment> results =
            treatmentRepository
                    .findByAnimalIdOrderByPerformedAtAsc(
                            animal.getId()
                    );

    assertThat(results).hasSize(3);

    assertThat(results)
            .extracting(Treatment::getType)
            .containsExactly(
                    TreatmentType.WOUND_CARE,
                    TreatmentType.HYDRATION,
                    TreatmentType.OBSERVATION
            );
}

@Test
void shouldFindTreatmentsBetweenDatesUsingJpql() {
    RescueCenter center = new RescueCenter(
            "DB-DATE",
            "DeepBlue Date Center",
            "Santa Marta"
    );

    RescueCase rescueCase = new RescueCase(
            "RES-DATE-001",
            LocalDate.of(2026, 8, 1),
            "Playa Grande",
            RescueStatus.IN_REHABILITATION
    );

    Animal animal = new Animal(
            "AN-DATE-001",
            "Marine Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
    );

    center.addCase(rescueCase);
    rescueCase.assignAnimal(animal);
    rescueCenterRepository.saveAndFlush(center);

    Specialist specialist = new Specialist(
            "SPEC-DATE-001",
            "Elena",
            "Vargas",
            "elena.date@deepblue.org",
            true
    );

    specialistRepository.saveAndFlush(specialist);

    Treatment first = new Treatment(
            animal,
            specialist,
            LocalDateTime.of(2026, 8, 1, 10, 0),
            TreatmentType.OBSERVATION,
            "First treatment"
    );

    Treatment middle = new Treatment(
            animal,
            specialist,
            LocalDateTime.of(2026, 8, 10, 10, 0),
            TreatmentType.HYDRATION,
            "Middle treatment"
    );

    Treatment last = new Treatment(
            animal,
            specialist,
            LocalDateTime.of(2026, 8, 20, 10, 0),
            TreatmentType.NUTRITION,
            "Last treatment"
    );

    treatmentRepository.saveAllAndFlush(
            List.of(first, middle, last)
    );

    List<Treatment> results =
            treatmentRepository.findBetweenDates(
                    LocalDateTime.of(2026, 8, 5, 0, 0),
                    LocalDateTime.of(2026, 8, 15, 23, 59)
            );

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getPerformedAt())
            .isEqualTo(LocalDateTime.of(2026, 8, 10, 10, 0));
}

@Test
void shouldRejectDuplicatedAnimalCode() {
    RescueCenter firstCenter = new RescueCenter(
            "DB-UNIQUE-1",
            "DeepBlue Unique One",
            "Santa Marta"
    );

    RescueCase firstCase = new RescueCase(
            "RES-UNIQUE-001",
            LocalDate.of(2026, 8, 25),
            "Taganga",
            RescueStatus.ADMITTED
    );

    Animal firstAnimal = new Animal(
            "AN-100",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
    );

    firstCenter.addCase(firstCase);
    firstCase.assignAnimal(firstAnimal);
    rescueCenterRepository.saveAndFlush(firstCenter);

    RescueCenter secondCenter = new RescueCenter(
            "DB-UNIQUE-2",
            "DeepBlue Unique Two",
            "Santa Marta"
    );

    RescueCase secondCase = new RescueCase(
            "RES-UNIQUE-002",
            LocalDate.of(2026, 8, 26),
            "Playa Grande",
            RescueStatus.ADMITTED
    );

    Animal duplicatedAnimal = new Animal(
            "AN-100",
            "Marine Turtle",
            "Chelonia mydas",
            AnimalSex.UNKNOWN
    );

    secondCenter.addCase(secondCase);
    secondCase.assignAnimal(duplicatedAnimal);

    assertThatThrownBy(() ->
            rescueCenterRepository.saveAndFlush(secondCenter)
    ).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void shouldRejectInvalidForeignKeys() {
    assertThatThrownBy(() ->
            jdbcTemplate.update(
                    """
                    INSERT INTO treatments (
                        animal_id,
                        specialist_id,
                        performed_at,
                        type,
                        description
                    )
                    VALUES (
                        999999,
                        999999,
                        CURRENT_TIMESTAMP,
                        'OBSERVATION',
                        'Invalid foreign keys'
                    )
                    """
            )
    ).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void shouldRejectInvalidRescueStatus() {
    RescueCenter center = new RescueCenter(
            "DB-CHECK",
            "DeepBlue Check Center",
            "Santa Marta"
    );

    rescueCenterRepository.saveAndFlush(center);

    assertThatThrownBy(() ->
            jdbcTemplate.update(
                    """
                    INSERT INTO rescue_cases (
                        case_code,
                        rescue_date,
                        rescue_location,
                        status,
                        rescue_center_id
                    )
                    VALUES (
                        'RES-CHECK-INVALID',
                        DATE '2026-08-30',
                        'Santa Marta',
                        'INVALID_STATUS',
                        ?
                    )
                    """,
                    center.getId()
            )
    ).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void shouldPersistAndQueryCompleteDeepBlueScenario() {
    RescueCenter center = new RescueCenter(
            "DB-CAR",
            "DeepBlue Caribbean",
            "Santa Marta"
    );

    RescueCase rescueCase = new RescueCase(
            "RES-2026-100",
            LocalDate.of(2026, 8, 18),
            "Bahía Concha",
            RescueStatus.IN_REHABILITATION
    );

    Animal animal = new Animal(
            "AN-2026-100",
            "Green Sea Turtle",
            "Chelonia mydas",
            AnimalSex.FEMALE
    );

    animal.setTrackingDeviceCode("GPS-AN-2026-100");

    MedicalRecord medicalRecord = new MedicalRecord(
            new BigDecimal("27.80"),
            "STABLE",
            "Injury caused by fishing net",
            "Possible plastic ingestion"
    );

    center.addCase(rescueCase);
    rescueCase.assignAnimal(animal);
    animal.assignMedicalRecord(medicalRecord);

    rescueCenterRepository.saveAndFlush(center);

    Expertise marineReptiles = expertiseRepository
            .findByNameIgnoreCase("Marine Reptiles")
            .orElseThrow();

    Expertise trauma = expertiseRepository
            .findByNameIgnoreCase("Trauma")
            .orElseThrow();

    Expertise rehabilitation = expertiseRepository
            .findByNameIgnoreCase("Rehabilitation")
            .orElseThrow();

    Specialist elena = new Specialist(
            "SPEC-001",
            "Elena",
            "Vargas",
            "elena@deepblue.org",
            true
    );

    elena.addExpertise(marineReptiles);
    elena.addExpertise(trauma);
    elena.addExpertise(rehabilitation);

    specialistRepository.saveAndFlush(elena);

    Treatment woundCare = new Treatment(
            animal,
            elena,
            LocalDateTime.of(2026, 8, 19, 9, 0),
            TreatmentType.WOUND_CARE,
            "Cleaning of left front flipper"
    );

    Treatment hydration = new Treatment(
            animal,
            elena,
            LocalDateTime.of(2026, 8, 20, 10, 0),
            TreatmentType.HYDRATION,
            "Subcutaneous fluid therapy"
    );

    treatmentRepository.saveAllAndFlush(
            List.of(woundCare, hydration)
    );

    assertThat(
            rescueCaseRepository.findByCaseCode("RES-2026-100")
    ).isPresent();

    assertThat(
            rescueCaseRepository
                    .findByStatusOrderByRescueDateAsc(
                            RescueStatus.IN_REHABILITATION
                    )
    ).extracting(RescueCase::getCaseCode)
            .contains("RES-2026-100");

    assertThat(
            animalRepository
                    .findByRescueCaseRescueCenterCode("DB-CAR")
    ).extracting(Animal::getAnimalCode)
            .containsExactly("AN-2026-100");

    assertThat(
            animalRepository
                    .findByCommonNameContainingIgnoreCase("turtle")
    ).extracting(Animal::getAnimalCode)
            .containsExactly("AN-2026-100");

    assertThat(
            specialistRepository.findActiveByExpertise("Trauma")
    ).extracting(Specialist::getFirstName)
            .containsExactly("Elena");

    assertThat(
            treatmentRepository
                    .findByAnimalIdOrderByPerformedAtAsc(
                            animal.getId()
                    )
    ).extracting(Treatment::getType)
            .containsExactly(
                    TreatmentType.WOUND_CARE,
                    TreatmentType.HYDRATION
            );

    assertThat(
            treatmentRepository
                    .findBySpecialistExpertise("Rehabilitation")
    ).hasSize(2);

    assertThat(
            treatmentRepository.findBetweenDates(
                    LocalDateTime.of(2026, 8, 19, 0, 0),
                    LocalDateTime.of(2026, 8, 20, 23, 59)
            )
    ).hasSize(2);

    assertThat(
            treatmentRepository.findByCenterCode("DB-CAR")
    ).hasSize(2);

    assertThat(
        animalRepository.findByStatusAndTreatmentExpertise(
                RescueStatus.IN_REHABILITATION,
                "trauma"
        )
).extracting(Animal::getAnimalCode)
        .containsExactly("AN-2026-100");
    
}
}
