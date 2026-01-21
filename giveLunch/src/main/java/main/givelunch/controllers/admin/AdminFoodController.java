package main.givelunch.controllers.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodDto;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.services.admin.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@RequestMapping("/api/admin/foods")
public class AdminFoodController {
    private final AdminService adminService;

    //post,put,delete에 상태코드 추가 필요
    @GetMapping
    public List<FoodDto> loadFoods() {
        return adminService.loadFoods();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void createFoodAndNutrition(@RequestBody FoodAndNutritionDto request) {
        Food food = adminService.saveFoodAndNutrition(request);
    }

    @DeleteMapping("/{id}")
    public void deleteFood(@PathVariable Long id) {
        adminService.deleteFoodsAndNutritions(id);
    }

    @PutMapping("/{id}")
    public void updateFoodAndNutrition(@PathVariable Long id, @RequestBody FoodAndNutritionDto request) {
        adminService.updateFoodAndNutrition(id,request);
    }
}
