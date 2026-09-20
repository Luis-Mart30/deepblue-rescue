package com.deepblue.rescue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "animal_id",
            nullable = false,
            unique = true
    )
    private Animal animal;

    @Column(
            name = "initial_weight",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal initialWeight;

    @Column(name = "initial_condition", nullable = false, length = 100)
    private String initialCondition;

    @Column(columnDefinition = "TEXT")
    private String injuries;

    @Column(columnDefinition = "TEXT")
    private String observations;

    public MedicalRecord() {
    }

    public MedicalRecord(
            BigDecimal initialWeight,
            String initialCondition,
            String injuries,
            String observations
    ) {
        this.initialWeight = initialWeight;
        this.initialCondition = initialCondition;
        this.injuries = injuries;
        this.observations = observations;
    }

    public Long getId() {
        return id;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public void setInitialWeight(BigDecimal initialWeight) {
        this.initialWeight = initialWeight;
    }

    public String getInitialCondition() {
        return initialCondition;
    }

    public void setInitialCondition(String initialCondition) {
        this.initialCondition = initialCondition;
    }

    public String getInjuries() {
        return injuries;
    }

    public void setInjuries(String injuries) {
        this.injuries = injuries;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}