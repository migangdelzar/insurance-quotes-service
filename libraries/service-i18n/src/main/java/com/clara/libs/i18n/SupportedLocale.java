package com.clara.libs.i18n;

import java.util.List;
import java.util.Locale;

public enum SupportedLocale {
    EN_US(Locale.forLanguageTag("en-US")),
    ES_MX(Locale.forLanguageTag("es-MX"));

    private final Locale locale;

    SupportedLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale locale() {
        return locale;
    }

    public static Locale fromHeader(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return EN_US.locale();
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(acceptLanguage);
        } catch (IllegalArgumentException exception) {
            return EN_US.locale();
        }
        var supported = List.of(EN_US.locale(), ES_MX.locale());
        var match = Locale.lookup(ranges, supported);
        return match != null ? match : EN_US.locale();
    }
}
