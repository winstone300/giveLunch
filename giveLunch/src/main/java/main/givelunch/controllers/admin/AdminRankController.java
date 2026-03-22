package main.givelunch.controllers.admin;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.rankDto.RankRebuildResultDto;
import main.givelunch.services.roulette.RankService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@RequestMapping("/api/admin/ranks")
public class AdminRankController {
    private final RankService rankService;

    @Operation(summary = "랭킹 수동 복구", description = "최근 retention 창 기준으로 rank를 events에서 다시 구성")
    @PostMapping("/rebuild")
    public RankRebuildResultDto rebuildRanks() {
        return rankService.rebuildRanks();
    }
}
