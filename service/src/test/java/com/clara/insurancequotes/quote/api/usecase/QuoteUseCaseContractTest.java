package com.clara.insurancequotes.quote.api.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.insurancequotes.quote.api.command.CreateQuoteCommand;
import com.clara.insurancequotes.quote.api.command.UpdateCoverageCommand;
import com.clara.insurancequotes.quote.api.query.SearchQuotesQuery;
import com.clara.insurancequotes.quote.api.result.QuoteDetails;
import com.clara.insurancequotes.quote.api.result.QuotePage;
import com.clara.insurancequotes.quote.api.result.QuoteSummary;
import com.clara.insurancequotes.quote.api.type.QuoteStatusView;
import com.clara.insurancequotes.quote.api.type.RequestingUser;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteUseCaseContractTest {

    @Test
    void exposesFocusedQuoteCapabilitiesWithStablePublicModels() throws NoSuchMethodException {
        assertSingleMethod(
                CreateQuoteUseCase.class, QuoteDetails.class, "create", CreateQuoteCommand.class, UUID.class);
        assertSingleMethod(
                UpdateCoverageUseCase.class,
                QuoteDetails.class,
                "updateCoverage",
                UUID.class,
                UpdateCoverageCommand.class,
                UUID.class);
        assertSingleMethod(GetQuoteUseCase.class, QuoteDetails.class, "getQuote", UUID.class, RequestingUser.class);
        assertSingleMethod(
                SearchQuotesUseCase.class,
                QuotePage.class,
                "searchQuotes",
                SearchQuotesQuery.class,
                RequestingUser.class);
        assertSingleMethod(GetQuoteSummaryUseCase.class, QuoteSummary.class, "getSummary", RequestingUser.class);

        assertSingleMethod(GetOwnedQuoteUseCase.class, QuoteDetails.class, "getOwnedQuote", UUID.class, UUID.class);
        assertSingleMethod(
                EnsureQuoteSubmittableUseCase.class, QuoteDetails.class, "ensureSubmittable", UUID.class, UUID.class);
        assertSingleMethod(
                MarkQuoteSubmittedUseCase.class, QuoteDetails.class, "markSubmitted", UUID.class, UUID.class);
        assertSingleMethod(
                MarkQuoteSubmissionFailedUseCase.class,
                QuoteDetails.class,
                "markSubmissionFailed",
                UUID.class,
                UUID.class);

        assertThat(QuoteDetails.class.getRecordComponents())
                .anyMatch(component -> component.getType().equals(QuoteStatusView.class));
        assertThat(QuotePage.class.getRecordComponents()[0].getGenericType().getTypeName())
                .contains(QuoteDetails.class.getName());
        assertThat(QuoteSummary.class.getSimpleName()).isEqualTo("QuoteSummary");
    }

    private static void assertSingleMethod(Class<?> type, Class<?> returnType, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = type.getMethod(name, parameterTypes);
        assertThat(type.getDeclaredMethods()).containsExactly(method);
        assertThat(method.getReturnType()).isEqualTo(returnType);
    }
}
