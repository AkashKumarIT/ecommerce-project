package com.ecom.cartservice.scheduler;

import com.ecom.cartservice.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupScheduler {

    private final IdempotencyRecordRepository repository;

    // Runs every hour
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldRecords() {

        Instant expiryTime = Instant.now().minus(24, ChronoUnit.HOURS);

        int deleted = repository.deleteOlderThan(expiryTime);

        if (deleted > 0) {
            log.info("Idempotency cleanup: deleted {} old records", deleted);
        }
    }
}