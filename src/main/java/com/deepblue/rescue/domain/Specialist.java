package com.deepblue.rescue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "professional_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String professionalCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private boolean active;

    @ManyToMany
    @JoinTable(
            name = "specialist_expertise",
            joinColumns = @JoinColumn(name = "specialist_id"),
            inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private Set<Expertise> expertiseAreas = new HashSet<>();

    @OneToMany(mappedBy = "specialist")
private List<Treatment> treatments = new ArrayList<>();

    public Specialist() {
    }

    public Specialist(
            String professionalCode,
            String firstName,
            String lastName,
            String email,
            boolean active
    ) {
        this.professionalCode = professionalCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.active = active;
    }

    public void addExpertise(Expertise expertise) {
        expertiseAreas.add(expertise);
        expertise.getSpecialists().add(this);
    }

    public Long getId() {
        return id;
    }

    public String getProfessionalCode() {
        return professionalCode;
    }

    public void setProfessionalCode(String professionalCode) {
        this.professionalCode = professionalCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Expertise> getExpertiseAreas() {
        return expertiseAreas;
    }

    public List<Treatment> getTreatments() {
    return treatments;
}
}