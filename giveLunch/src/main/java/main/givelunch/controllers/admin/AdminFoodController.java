package main.givelunch.controllers.admin;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.admin.AdminFoodImportPreviewResponse;
import main.givelunch.dto.admin.AdminFoodImportRequest;
import main.givelunch.dto.admin.AdminFoodImportResponse;
import main.givelunch.dto.FoodAndNutritionDto.FoodDto;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.services.admin.AdminFoodImportService;
import main.givelunch.services.admin.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@RequestMapping("/api/admin/foods")
public class AdminFoodController {
    private final AdminService adminService;
    private final AdminFoodImportService adminFoodImportService;

    @Operation(summary = "음식 목록 조회", description = "관리자용 음식 목록을 페이지 단위로 조회")
    @GetMapping
    public Page<FoodDto> loadFoods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return adminService.loadFoods(page, size, keyword);
    }

    @Operation(summary = "음식 및 영양 정보 생성", description = "관리자 권한으로 음식과 영양 정보를 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createFoodAndNutrition(@RequestBody FoodAndNutritionDto request) {
        adminService.saveFoodAndNutrition(request);
    }

    @Operation(summary = "음식 파일 등록 미리보기", description = "관리자 권한으로 CSV/XLSX 음식 파일을 파싱해 미리보기")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminFoodImportPreviewResponse previewFoodImport(@RequestPart("file") MultipartFile file) {
        return adminFoodImportService.preview(file);
    }

    @Operation(summary = "음식 파일 일괄 등록", description = "관리자 권한으로 미리보기에서 확정한 음식들을 일괄 등록")
    @PostMapping("/import")
    public AdminFoodImportResponse importFoods(@RequestBody AdminFoodImportRequest request) {
        return adminFoodImportService.importFoods(request);
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
