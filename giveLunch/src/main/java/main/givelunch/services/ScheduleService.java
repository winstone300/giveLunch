package main.givelunch.services;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import main.givelunch.repositories.EmailVerificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final EmailVerificationRepository emailVerificationRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredCodes() {
        emailVerificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
