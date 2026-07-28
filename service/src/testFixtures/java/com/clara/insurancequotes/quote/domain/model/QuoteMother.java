package com.clara.insurancequotes.quote.domain.model;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class QuoteMother {

    public static final Instant FIXED_NOW = Instant.parse("2026-07-22T10:00:00Z");
    public static final UUID OWNER_ID = UUID.fromString("a1111111-0000-0000-0000-000000000001");

    private QuoteMother() {}

    public static Quote draft() {
        return draftForOwner(OWNER_ID);
    }

    public static Quote draftForOwner(UUID ownerId) {
        return Quote.createDraft(ownerId, "Jane Roe", "jane@example.com", 34, "06600", FIXED_NOW);
    }

    public static Quote seniorDraft() {
        return seniorDraftForOwner(OWNER_ID);
    }

    public static Quote seniorDraftForOwner(UUID ownerId) {
        return Quote.createDraft(ownerId, "John Elder", "john@example.com", 70, "06600", FIXED_NOW);
    }

    public static Quote submittableDraft() {
        return submittableDraft(OWNER_ID);
    }

    public static Quote submittableDraft(UUID ownerId) {
        var quote = draftForOwner(ownerId);
        quote.updateCoverage(CoverageType.STANDARD, HealthProfile.none(), new BigDecimal("100.00"), FIXED_NOW);
        return quote;
    }

    public static Quote submittableSeniorDraft() {
        return submittableSeniorDraft(OWNER_ID);
    }

    public static Quote submittableSeniorDraft(UUID ownerId) {
        var quote = seniorDraftForOwner(ownerId);
        var health = new HealthProfile(true, Set.of(HealthCondition.DIABETES), false, true, true);
        quote.updateCoverage(CoverageType.STANDARD, health, new BigDecimal("327.60"), FIXED_NOW);
        return quote;
    }
}
