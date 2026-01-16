package main.givelunch.controllers;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.services.roulette.FoodNutritionService;
import main.givelunch.services.roulette.FoodSearchService;
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
    public Long getId(@RequestParam("name") String name) {
        return foodSearchService.getIdByName(name);
    }

}
