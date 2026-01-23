package main.givelunch.controllers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.services.external.DataGoKrFoodClient;
import main.givelunch.services.roulette.FoodNutritionService;
import main.givelunch.services.roulette.FoodSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    // get요청은 굳이 ResponseEntity로 안감싸도 될거같음
    @GetMapping("/{foodId}/nutrition")
    public FoodAndNutritionDto getFoodNutrition(@PathVariable Long foodId) {
        return foodNutritionService.getFoodNutrition(foodId);
    }

    @GetMapping("/getId")
    public Long getId(@RequestParam("name") String name) {
       return foodSearchService.getIdByName(name);
    }

    @GetMapping("/external")
    public List<FoodAndNutritionDto> getExternalFood(@RequestParam("name") String name,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {
        List<FoodAndNutritionDto> results = foodSearchService.searchExternalFoods(name,userDetails);
        return results.isEmpty() ? List.of() : results;
    }
}
