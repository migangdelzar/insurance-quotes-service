package com.clara.insurancequotes.quote.application.mapper;

import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.util.Set;

/** Translates Quote domain models into stable Quote API result models. */
public final class QuoteApplicationMapper {

    private QuoteApplicationMapper() {}

    public static QuoteDetails toDetails(Quote quote) {
        var health = quote.healthProfile();
        return new QuoteDetails(
                quote.id(),
                quote.name(),
                quote.email(),
                quote.age(),
                quote.zipCode(),
                quote.coverageType(),
                health.hasPreexistingConditions(),
                copyConditions(health.conditions()),
                health.takesPrescriptionMedication(),
                health.usesTobacco(),
                health.needsSpouseCoverage(),
                quote.monthlyPremium(),
                toStatusView(quote.status()),
                quote.createdAt(),
                quote.updatedAt());
    }

    private static Set<com.clara.insurancequotes.quote.api.type.HealthCondition> copyConditions(
            Set<com.clara.insurancequotes.quote.api.type.HealthCondition> conditions) {
        return conditions == null ? null : Set.copyOf(conditions);
    }

    private static QuoteStatusView toStatusView(com.clara.insurancequotes.quote.domain.model.QuoteStatus status) {
        return switch (status) {
            case DRAFT -> QuoteStatusView.DRAFT;
            case SUBMITTED -> QuoteStatusView.SUBMITTED;
            case SUBMISSION_FAILED -> QuoteStatusView.SUBMISSION_FAILED;
            case EXPIRED -> QuoteStatusView.EXPIRED;
        };
    }
}
