package main.givelunch.controllers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.RankEntryDto;
import main.givelunch.dto.RankRecordRequestDto;
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

    @PostMapping
    public RankEntryDto recordRank(@RequestBody RankRecordRequestDto request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food name is required.");
        }
        String normalizedName = request.name().trim();
        return rankService.increment(normalizedName);
    }

    @GetMapping("/top")
    public List<RankEntryDto> topRanks(@RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 5);
        return rankService.getTopRanks(safeLimit);
    }
}