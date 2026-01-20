package dev.mgmeral.ticket.job;

import dev.mgmeral.ticket.enums.HoldStatus;
import dev.mgmeral.ticket.repository.HoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryJob {

    private final HoldRepository holdRepository;

    @Value("${holds.expiry-job.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${holds.expiry-job.fixed-delay-ms:30000}")
    @Transactional
    public void expireHolds() {
        if (!enabled) return;

        Instant now = Instant.now();
        int expired = holdRepository.expireAll(HoldStatus.HELD, HoldStatus.EXPIRED, now);

        if (expired > 0) {
            log.info("hold_expiry_job expiredCount={} now={}", expired, now);
        }
    }
}
