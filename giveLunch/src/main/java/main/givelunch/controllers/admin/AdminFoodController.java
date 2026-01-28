package main.givelunch.controllers.admin;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodDto;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.services.admin.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "음식 목록 조회", description = "관리자용 음식 목록을 조회")
    @GetMapping
    public List<FoodDto> loadFoods() {
        return adminService.loadFoods();
    }

    @Operation(summary = "음식 및 영양 정보 생성", description = "관리자 권한으로 음식과 영양 정보를 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createFoodAndNutrition(@RequestBody FoodAndNutritionDto request) {
        adminService.saveFoodAndNutrition(request);
    }

    @Operation(summary = "음식 삭제", description = "관리자 권한으로 음식과 영양 정보를 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteFood(@PathVariable Long id) {
        adminService.deleteFoodsAndNutritions(id);
    }

    @Operation(summary = "음식 및 영양 정보 수정", description = "관리자 권한으로 음식과 영양 정보를 수정")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{id}")
    public void updateFoodAndNutrition(@PathVariable Long id, @RequestBody FoodAndNutritionDto request) {
        adminService.updateFoodAndNutrition(id, request);
    }
}
