package com.clara.libs.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class MessageResolverTest {

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final MessageResolver resolver = new MessageResolver(messageSource);

    @Test
    void resolvesLocalizedMessageWithArgs() {
        messageSource.addMessage(
                "error.QUOTE_NOT_FOUND", Locale.forLanguageTag("es-MX"), "Cotización {0} no encontrada");

        var message = resolver.resolve("error.QUOTE_NOT_FOUND", Locale.forLanguageTag("es-MX"), "abc");

        assertThat(message).isEqualTo("Cotización abc no encontrada");
    }

    @Test
    void unknownCode_fallsBackToCodeItself() {
        assertThat(resolver.resolve("error.UNKNOWN", Locale.US)).isEqualTo("error.UNKNOWN");
    }

    @Test
    void headerResolution_defaultsToEnUs_andMatchesEsMx() {
        assertThat(SupportedLocale.fromHeader(null)).isEqualTo(Locale.forLanguageTag("en-US"));
        assertThat(SupportedLocale.fromHeader("es-MX,es;q=0.9")).isEqualTo(Locale.forLanguageTag("es-MX"));
        assertThat(SupportedLocale.fromHeader("fr-FR")).isEqualTo(Locale.forLanguageTag("en-US"));
    }

    @Test
    void headerResolution_malformedLanguageRangeFallsBackToEnUs() {
        assertThat(SupportedLocale.fromHeader("-")).isEqualTo(Locale.forLanguageTag("en-US"));
    }
}
