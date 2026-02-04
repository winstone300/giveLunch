package main.givelunch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.rankDto.RankEntryDto;
import main.givelunch.dto.rankDto.RankRecordRequestDto;
import main.givelunch.services.roulette.RankService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranks")
public class RankController {
    private final RankService rankService;

    @Operation(summary = "먹은 건수 +1" ,description = "해당 음식 건수에 1건을 더함")
    @PostMapping
    public RankEntryDto recordRank(@RequestBody RankRecordRequestDto request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food name is required.");
        }
        String normalizedName = request.name().trim();
        return rankService.increment(normalizedName);
    }

    @Operation(summary = "랭킹 조회" , description = "상위 5개 음식과 건수 리스트 조회")
    @GetMapping("/top")
    public List<RankEntryDto> topRanks(@RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 5);
        return rankService.getTopRanks(safeLimit);
    }
}