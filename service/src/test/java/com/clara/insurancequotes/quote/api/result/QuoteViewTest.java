package com.clara.insurancequotes.quote.api.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import com.clara.insurancequotes.quote.domain.model.HealthProfile;
import com.clara.insurancequotes.quote.domain.model.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuoteViewTest {

    @Test
    void fromCopiesHealthConditionsIntoTransportSafeCollection() {
        var conditions = new HashSet<>(Set.of(HealthCondition.DIABETES));
        var quote = Quote.createDraft("Jane Roe", "jane@example.com", 70, "06600", Instant.EPOCH);
        quote.updateCoverage(
                CoverageType.BASIC,
                new HealthProfile(true, conditions, false, false, false),
                new BigDecimal("100.00"),
                Instant.EPOCH);

        var view = QuoteView.from(quote);

        assertThat(view.conditions()).containsExactly(HealthCondition.DIABETES);
        assertThat(view.conditions()).isNotSameAs(conditions);
    }
}
