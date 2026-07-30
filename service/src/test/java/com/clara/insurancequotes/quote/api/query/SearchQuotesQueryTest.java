package com.clara.insurancequotes.quote.api.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SearchQuotesQueryTest {

    @Test
    void defaultsToRecentQuotesWithBoundedPageSize() {
        var query = SearchQuotesQuery.defaults();

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(SearchQuotesQuery.DEFAULT_SIZE);
        assertThat(query.sortBy()).isEqualTo(QuoteSortField.CREATED_AT);
        assertThat(query.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void normalizesFiltersAndParsesSupportedValues() {
        var query = SearchQuotesQuery.of(2, 10, "  Jane Roe  ", "submitted", "standard", "name", "ascending");

        assertThat(query.search()).isEqualTo("Jane Roe");
        assertThat(query.status().name()).isEqualTo("SUBMITTED");
        assertThat(query.coverage().name()).isEqualTo("STANDARD");
        assertThat(query.sortBy()).isEqualTo(QuoteSortField.NAME);
        assertThat(query.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsPageSizeAboveTheConfiguredBound() {
        assertThatThrownBy(() -> SearchQuotesQuery.of(0, SearchQuotesQuery.MAX_SIZE + 1, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between");
    }
}
