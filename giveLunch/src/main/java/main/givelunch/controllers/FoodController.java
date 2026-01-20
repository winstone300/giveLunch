package main.givelunch.controllers;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.FoodIdResponseDto;
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

    @GetMapping("/{foodId}/nutrition")
    public FoodAndNutritionDto getFoodNutrition(@PathVariable Long foodId) {
        return foodNutritionService.getFoodNutrition(foodId);
    }

    @GetMapping("/getId")
    public ResponseEntity<FoodIdResponseDto> getId(@RequestParam("name") String name) {
        Long id = foodSearchService.getIdByName(name);
        return ResponseEntity.ok(new FoodIdResponseDto(id));
    }

}
