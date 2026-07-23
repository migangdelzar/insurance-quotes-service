package com.clara.insurancequotes.quote.domain.model;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.exception.IncompleteQuoteException;
import com.clara.insurancequotes.quote.domain.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private int age;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_type")
    private CoverageType coverageType;

    @Embedded
    private HealthProfile healthProfile;

    @Column(name = "monthly_premium")
    private BigDecimal monthlyPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Quote() {}

    private Quote(UUID id, String name, String email, int age, String zipCode, Instant now) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.zipCode = zipCode;
        this.status = QuoteStatus.DRAFT;
        this.healthProfile = HealthProfile.none();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Quote createDraft(String name, String email, int age, String zipCode, Instant now) {
        return new Quote(UUID.randomUUID(), name, email, age, zipCode, now);
    }

    public void updateCoverage(
            CoverageType coverageType, HealthProfile healthProfile, BigDecimal premium, Instant now) {
        if (!status.allowsCoverageUpdate()) {
            throw new InvalidStateTransitionException(id, status.name(), "COVERAGE_UPDATE");
        }
        this.coverageType = coverageType;
        this.healthProfile = healthProfile;
        this.monthlyPremium = premium;
        this.updatedAt = now;
    }

    public void ensureSubmittable() {
        if (!status.allowsSubmission()) {
            throw new InvalidStateTransitionException(id, status.name(), "SUBMIT");
        }
        if (coverageType == null) {
            throw new IncompleteQuoteException(id, "coverage type not selected");
        }
    }

    public void markSubmitted(Instant now) {
        ensureSubmittable();
        this.status = QuoteStatus.SUBMITTED;
        this.updatedAt = now;
    }

    public void markSubmissionFailed(Instant now) {
        if (!status.allowsSubmission()) {
            throw new InvalidStateTransitionException(id, status.name(), "SUBMISSION_FAILURE");
        }
        this.status = QuoteStatus.SUBMISSION_FAILED;
        this.updatedAt = now;
    }

    public void expire(Instant now) {
        if (!status.allowsExpiration()) {
            throw new InvalidStateTransitionException(id, status.name(), "EXPIRE");
        }
        this.status = QuoteStatus.EXPIRED;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public int age() {
        return age;
    }

    public String zipCode() {
        return zipCode;
    }

    public CoverageType coverageType() {
        return coverageType;
    }

    public HealthProfile healthProfile() {
        return healthProfile;
    }

    public BigDecimal monthlyPremium() {
        return monthlyPremium;
    }

    public QuoteStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
