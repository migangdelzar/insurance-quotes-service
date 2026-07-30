package com.clara.insurancequotes.quote.api.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.api.type.HealthCondition;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteDetailsTest {

    @Test
    void copiesHealthConditionsIntoTransportSafeCollection() {
        var conditions = new HashSet<>(Set.of(HealthCondition.DIABETES));
        var details = new QuoteDetails(
                UUID.randomUUID(),
                "Jane Roe",
                "jane@example.com",
                70,
                "06600",
                CoverageType.BASIC,
                true,
                conditions,
                false,
                false,
                false,
                new BigDecimal("100.00"),
                QuoteStatusView.DRAFT,
                Instant.EPOCH,
                Instant.EPOCH);
        conditions.clear();

        assertThat(details.conditions()).containsExactly(HealthCondition.DIABETES);
        assertThat(details.conditions()).isNotSameAs(conditions);
    }
}
