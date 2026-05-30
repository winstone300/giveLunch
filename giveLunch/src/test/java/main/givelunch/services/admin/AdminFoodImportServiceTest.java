package main.givelunch.services.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.FoodDto;
import main.givelunch.dto.admin.AdminFoodImportItem;
import main.givelunch.dto.admin.AdminFoodImportPreviewResponse;
import main.givelunch.dto.admin.AdminFoodImportRequest;
import main.givelunch.dto.admin.AdminFoodImportResponse;
import main.givelunch.dto.admin.AdminFoodImportStatus;
import main.givelunch.entities.Food;
import main.givelunch.repositories.FoodRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminFoodImportServiceTest {

    @Mock
    private AdminService adminService;

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private AdminFoodImportService adminFoodImportService;

    @Test
    @DisplayName("preview - CSV 파일을 파싱하고 미지원 카테고리는 기타로 정규화")
    void previewCsvNormalizesUnsupportedCategoryAndInvalidRows() {
        String csv = "name,category,servingSizeG,calories,carbohydrate,protein,fat\n"
                + "김밥,분식,200,350,55,12,8\n"
                + ",한식,100,120,20,5,2\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foods.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        AdminFoodImportPreviewResponse response = adminFoodImportService.preview(file);

        assertThat(response.rows()).hasSize(2);
        assertThat(response.validCount()).isEqualTo(1);
        assertThat(response.invalidCount()).isEqualTo(1);
        assertThat(response.rows().get(0).rowNumber()).isEqualTo(1);
        assertThat(response.rows().get(1).rowNumber()).isEqualTo(2);
        assertThat(response.rows().get(0).name()).isEqualTo("김밥");
        assertThat(response.rows().get(0).category()).isEqualTo("기타");
        assertThat(response.rows().get(0).nutrition().calories()).isEqualByComparingTo("350");
        assertThat(response.rows().get(1).valid()).isFalse();
        assertThat(response.rows().get(1).reason()).isEqualTo("음식 이름은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("preview - XLSX 첫 번째 시트의 헤더와 숫자 영양정보를 파싱")
    void previewXlsxParsesFirstSheet() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foods.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildXlsx()
        );

        AdminFoodImportPreviewResponse response = adminFoodImportService.preview(file);

        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).rowNumber()).isEqualTo(1);
        assertThat(response.rows().get(0).name()).isEqualTo("파스타");
        assertThat(response.rows().get(0).category()).isEqualTo("양식");
        assertThat(response.rows().get(0).servingSizeG()).isEqualTo(250);
        assertThat(response.rows().get(0).nutrition().protein()).isEqualByComparingTo("21");
    }

    @Test
    @DisplayName("importFoods - 신규 저장, 중복 skip, invalid 실패를 각각 반환")
    void importFoodsSavesSkipsAndFailsPerItem() {
        Food savedFood = Food.from(FoodDto.builder()
                .id(10L)
                .name("신규음식")
                .category("한식")
                .build());

        when(foodRepository.findIdByName("신규음식")).thenReturn(Optional.empty());
        when(foodRepository.findIdByName("기존음식")).thenReturn(Optional.of(3L));
        when(adminService.saveFoodAndNutrition(any(FoodAndNutritionDto.class))).thenReturn(savedFood);

        AdminFoodImportResponse response = adminFoodImportService.importFoods(new AdminFoodImportRequest(java.util.List.of(
                new AdminFoodImportItem(1, "신규음식", "한식", null, null, null),
                new AdminFoodImportItem(2, "기존음식", "양식", null, null, null),
                new AdminFoodImportItem(3, " ", "일식", null, null, null)
        )));

        assertThat(response.savedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.results().get(0).status()).isEqualTo(AdminFoodImportStatus.SAVED);
        assertThat(response.results().get(1).status()).isEqualTo(AdminFoodImportStatus.SKIPPED);
        assertThat(response.results().get(2).status()).isEqualTo(AdminFoodImportStatus.FAILED);

        ArgumentCaptor<FoodAndNutritionDto> captor = ArgumentCaptor.forClass(FoodAndNutritionDto.class);
        verify(adminService).saveFoodAndNutrition(captor.capture());
        assertThat(captor.getValue().category()).isEqualTo("한식");
        verify(adminService, never()).saveFoodAndNutrition(new FoodAndNutritionDto(null, "기존음식", "양식", null, null, null, null));
    }

    @Test
    @DisplayName("importFoods - overwriteExisting=true이면 기존 음식을 덮어쓴다")
    void importFoodsOverwritesDuplicateWhenRequested() {
        when(foodRepository.findIdByName("기존음식")).thenReturn(Optional.of(7L));

        AdminFoodImportResponse response = adminFoodImportService.importFoods(new AdminFoodImportRequest(java.util.List.of(
                new AdminFoodImportItem(2, "기존음식", "양식", "https://img.example.com/new.png", 300, null)
        ), true));

        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.results().get(0).status()).isEqualTo(AdminFoodImportStatus.UPDATED);
        assertThat(response.results().get(0).foodId()).isEqualTo(7L);

        ArgumentCaptor<FoodAndNutritionDto> captor = ArgumentCaptor.forClass(FoodAndNutritionDto.class);
        verify(adminService).updateFoodAndNutrition(eq(7L), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("기존음식");
        assertThat(captor.getValue().category()).isEqualTo("양식");
        assertThat(captor.getValue().imgUrl()).isEqualTo("https://img.example.com/new.png");
        verify(adminService, never()).saveFoodAndNutrition(any(FoodAndNutritionDto.class));
    }

    private byte[] buildXlsx() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("foods");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("카테고리");
            header.createCell(2).setCellValue("1회제공량g");
            header.createCell(3).setCellValue("칼로리");
            header.createCell(4).setCellValue("탄수화물");
            header.createCell(5).setCellValue("단백질");
            header.createCell(6).setCellValue("지방");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("파스타");
            row.createCell(1).setCellValue("양식");
            row.createCell(2).setCellValue(250);
            row.createCell(3).setCellValue(640);
            row.createCell(4).setCellValue(78);
            row.createCell(5).setCellValue(21);
            row.createCell(6).setCellValue(23);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
