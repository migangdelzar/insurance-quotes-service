package com.clara.insurancequotes.quote.infrastructure.inbound.scheduling;

import com.clara.insurancequotes.quote.application.scheduler.DraftExpirationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ExpirationScheduleConfig {

    private final DraftExpirationJob job;

    @Scheduled(fixedDelayString = "${quote.expiration.check-interval}")
    public void triggerExpiration() {
        job.expireStaleDrafts();
    }
}
