package com.clara.insurancequotes.quote.domain.model;

import com.clara.insurancequotes.quote.api.model.HealthCondition;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.Set;

@Embeddable
public class HealthProfile {

    @Column(name = "has_preexisting_conditions")
    private Boolean hasPreexistingConditions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_health_conditions", joinColumns = @JoinColumn(name = "quote_id"))
    @Column(name = "condition")
    @Enumerated(EnumType.STRING)
    private Set<HealthCondition> conditions;

    @Column(name = "takes_prescription_medication")
    private Boolean takesPrescriptionMedication;

    @Column(name = "uses_tobacco")
    private Boolean usesTobacco;

    @Column(name = "needs_spouse_coverage")
    private Boolean needsSpouseCoverage;

    protected HealthProfile() {}

    public HealthProfile(
            Boolean hasPreexistingConditions,
            Set<HealthCondition> conditions,
            Boolean takesPrescriptionMedication,
            Boolean usesTobacco,
            Boolean needsSpouseCoverage) {
        this.hasPreexistingConditions = hasPreexistingConditions;
        this.conditions = conditions;
        this.takesPrescriptionMedication = takesPrescriptionMedication;
        this.usesTobacco = usesTobacco;
        this.needsSpouseCoverage = needsSpouseCoverage;
    }

    public static HealthProfile none() {
        return new HealthProfile(null, null, null, null, null);
    }

    public boolean isPresent() {
        return hasPreexistingConditions != null
                || takesPrescriptionMedication != null
                || usesTobacco != null
                || needsSpouseCoverage != null;
    }

    public boolean anyConditionSelected() {
        return Boolean.TRUE.equals(hasPreexistingConditions);
    }

    public boolean tobaccoUse() {
        return Boolean.TRUE.equals(usesTobacco);
    }

    public boolean spouseCoverage() {
        return Boolean.TRUE.equals(needsSpouseCoverage);
    }

    public Boolean hasPreexistingConditions() {
        return hasPreexistingConditions;
    }

    public Set<HealthCondition> conditions() {
        return conditions;
    }

    public Boolean takesPrescriptionMedication() {
        return takesPrescriptionMedication;
    }

    public Boolean usesTobacco() {
        return usesTobacco;
    }

    public Boolean needsSpouseCoverage() {
        return needsSpouseCoverage;
    }
}
