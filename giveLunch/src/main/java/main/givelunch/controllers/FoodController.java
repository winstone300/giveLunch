package main.givelunch.controllers;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.services.external.DataGoKrFoodClient;
import main.givelunch.services.roulette.FoodNutritionService;
import main.givelunch.services.roulette.FoodSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodNutritionService foodNutritionService;
    private final FoodSearchService foodSearchService;
    private final DataGoKrFoodClient dataGoKrFoodClient;

    @GetMapping("/{foodId}/nutrition")
    public ResponseEntity<FoodAndNutritionDto> getFoodNutrition(@PathVariable Long foodId) {
        FoodAndNutritionDto dto = foodNutritionService.getFoodNutrition(foodId).orElse(null);

        if (dto != null) {
            return ResponseEntity.ok(dto); // 데이터가 있으면 JSON 반환
        } else {
            return ResponseEntity.notFound().build(); // 없으면 404 반환
        }
    }

    @GetMapping("/getId")
    public ResponseEntity<Long> getId(@RequestParam("name") String name) {
        Long id = foodSearchService.getIdByName(name);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/external")
    public ResponseEntity<FoodAndNutritionDto> getExternalFood(@RequestParam("name") String name) {
        FoodAndNutritionDto dto = dataGoKrFoodClient.fetchFoodByName(name).orElse(null);

        if (dto != null) {
            return ResponseEntity.ok(dto); // 데이터가 있으면 JSON 반환
        } else {
            return ResponseEntity.notFound().build(); // 없으면 404 반환
        }
    }
}
