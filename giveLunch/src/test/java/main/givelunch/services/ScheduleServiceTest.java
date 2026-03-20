package main.givelunch.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.services.roulette.RankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService")
class ScheduleServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private RankService rankService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    @DisplayName("cleanupExpiredCodes - 현재 시간 이전 만료코드를 삭제")
    void cleanupExpiredCodes_deletesExpiredCodes() {
        // when
        scheduleService.cleanupExpiredCodes();

        // then
        verify(emailVerificationRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("cleanupExpiredRanks - 랭킹 cleanup을 호출")
    void cleanupExpiredRanks_runsRankCleanup() {
        scheduleService.cleanupExpiredRanks();

        verify(rankService).cleanupExpiredRanks();
    }
}
