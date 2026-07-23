package com.clara.insurancequotes.shared.configuration;

import com.clara.libs.i18n.MessageResolver;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    @Bean
    public MessageResolver messageResolver(MessageSource messageSource) {
        return new MessageResolver(messageSource);
    }
}
