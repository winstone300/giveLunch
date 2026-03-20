package main.givelunch.services;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.services.roulette.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private final EmailVerificationRepository emailVerificationRepository;
    private final RankService rankService;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredCodes() {
        emailVerificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredRanks() {
        long removedCount = rankService.cleanupExpiredRanks();
        log.info("Scheduled rank cleanup completed. removedEventCount={}", removedCount);
    }
}
