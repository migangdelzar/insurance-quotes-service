package com.clara.libs.i18n;

import java.util.Locale;
import org.springframework.context.MessageSource;

public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
