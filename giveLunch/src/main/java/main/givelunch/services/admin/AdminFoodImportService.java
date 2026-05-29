package main.givelunch.services.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.NutritionDto;
import main.givelunch.dto.admin.AdminFoodImportItem;
import main.givelunch.dto.admin.AdminFoodImportPreviewResponse;
import main.givelunch.dto.admin.AdminFoodImportPreviewRow;
import main.givelunch.dto.admin.AdminFoodImportRequest;
import main.givelunch.dto.admin.AdminFoodImportResponse;
import main.givelunch.dto.admin.AdminFoodImportResult;
import main.givelunch.entities.Food;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.repositories.FoodRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminFoodImportService {
    private static final String DUPLICATE_REASON = "이미 같은 이름의 음식이 존재합니다.";
    private static final String INVALID_NAME_REASON = "음식 이름은 비어 있을 수 없습니다.";
    private static final Set<String> PRIMARY_CATEGORIES = Set.of("한식", "중식", "일식", "양식");

    private final AdminService adminService;
    private final FoodRepository foodRepository;

    public AdminFoodImportPreviewResponse preview(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            List<AdminFoodImportPreviewRow> rows;
            if (filename.endsWith(".csv")) {
                rows = parseCsv(file);
            } else if (filename.endsWith(".xlsx")) {
                rows = parseXlsx(file);
            } else {
                throw new ValidationException(ErrorCode.VALIDATION_ERROR, "CSV 또는 XLSX 파일만 등록할 수 있습니다.");
            }
            return AdminFoodImportPreviewResponse.from(rows);
        } catch (IOException e) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "파일을 읽을 수 없습니다.");
        }
    }

    public AdminFoodImportResponse importFoods(AdminFoodImportRequest request) {
        List<AdminFoodImportResult> results = new ArrayList<>();
        List<AdminFoodImportItem> items = request == null || request.items() == null
                ? List.of()
                : request.items();

        for (AdminFoodImportItem item : items) {
            results.add(importSingle(item));
        }

        return AdminFoodImportResponse.from(results);
    }

    private AdminFoodImportResult importSingle(AdminFoodImportItem item) {
        Integer rowNumber = item == null ? null : item.rowNumber();
        String name = normalizeName(item == null ? null : item.name());
        if (name == null) {
            return AdminFoodImportResult.failed(rowNumber, null, INVALID_NAME_REASON);
        }

        Optional<Long> existingId = foodRepository.findIdByName(name);
        if (existingId.isPresent()) {
            return AdminFoodImportResult.skipped(rowNumber, name, existingId.get(), DUPLICATE_REASON);
        }

        FoodAndNutritionDto dto = FoodAndNutritionDto.of(
                null,
                name,
                normalizeCategory(item.category()),
                normalizeBlankToNull(item.imgUrl()),
                item.servingSizeG(),
                item.nutrition(),
                "admin-file-import"
        );
        Food savedFood = adminService.saveFoodAndNutrition(dto);
        return AdminFoodImportResult.saved(rowNumber, name, savedFood.getId());
    }

    private List<AdminFoodImportPreviewRow> parseCsv(MultipartFile file) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (InputStream inputStream = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<AdminFoodImportPreviewRow> rows = new ArrayList<>();
            Map<String, String> headerLookup = buildHeaderLookup(parser.getHeaderMap().keySet());
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rows.add(buildPreviewRow(rowNumber++, headerLookup, header -> readCsv(record, header)));
            }
            return rows;
        }
    }

    private List<AdminFoodImportPreviewRow> parseXlsx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }

            Map<String, String> headerLookup = buildHeaderLookup(readXlsxHeaders(headerRow, formatter));
            List<AdminFoodImportPreviewRow> rows = new ArrayList<>();
            int rowNumber = 1;
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                rows.add(buildPreviewRow(rowNumber++, headerLookup, header -> readXlsx(row, header, formatter)));
            }
            return rows;
        }
    }

    private AdminFoodImportPreviewRow buildPreviewRow(
            int rowNumber,
            Map<String, String> headerLookup,
            HeaderValueReader reader
    ) {
        String reason = null;
        String name = normalizeName(readByAliases(headerLookup, reader, "name", "이름"));
        if (name == null) {
            reason = INVALID_NAME_REASON;
        }

        BigDecimal calories = null;
        BigDecimal carbohydrate = null;
        BigDecimal protein = null;
        BigDecimal fat = null;
        Integer servingSizeG = null;
        try {
            servingSizeG = parseIntegerOrNull(readByAliases(headerLookup, reader, "servingSizeG", "servingSizeg", "serving_sizeg", "1회제공량g", "1회 제공량(g)", "1회 제공량"));
            calories = parseDecimal(readByAliases(headerLookup, reader, "calories", "칼로리"));
            carbohydrate = parseDecimal(readByAliases(headerLookup, reader, "carbohydrate", "탄수화물"));
            protein = parseDecimal(readByAliases(headerLookup, reader, "protein", "단백질"));
            fat = parseDecimal(readByAliases(headerLookup, reader, "fat", "지방"));
        } catch (NumberFormatException e) {
            reason = "제공량과 영양 정보는 숫자로 입력해주세요.";
        }

        NutritionDto nutrition = hasAnyNutrition(calories, carbohydrate, protein, fat)
                ? NutritionDto.of(calories, protein, fat, carbohydrate)
                : null;

        return new AdminFoodImportPreviewRow(
                rowNumber,
                name,
                normalizeCategory(readByAliases(headerLookup, reader, "category", "카테고리")),
                normalizeBlankToNull(readByAliases(headerLookup, reader, "imgUrl", "imageUrl", "이미지URL", "이미지 URL")),
                servingSizeG,
                nutrition,
                reason == null,
                reason
        );
    }

    private Map<String, String> buildHeaderLookup(Iterable<String> headers) {
        Map<String, String> lookup = new LinkedHashMap<>();
        for (String header : headers) {
            lookup.put(normalizeHeader(header), header);
        }
        return lookup;
    }

    private List<String> readXlsxHeaders(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            headers.add(formatter.formatCellValue(cell));
        }
        return headers;
    }

    private String readByAliases(Map<String, String> headerLookup, HeaderValueReader reader, String... aliases) {
        for (String alias : aliases) {
            String header = headerLookup.get(normalizeHeader(alias));
            if (header != null) {
                return reader.read(header);
            }
        }
        return null;
    }

    private String readCsv(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header) : null;
    }

    private String readXlsx(Row row, String header, DataFormatter formatter) {
        Row headerRow = row.getSheet().getRow(row.getSheet().getFirstRowNum());
        if (headerRow == null) {
            return null;
        }
        for (Cell headerCell : headerRow) {
            if (header.equals(formatter.formatCellValue(headerCell))) {
                Cell valueCell = row.getCell(headerCell.getColumnIndex());
                return valueCell == null ? null : formatter.formatCellValue(valueCell);
            }
        }
        return null;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.replace("\uFEFF", "").replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        String value = normalizeBlankToNull(name);
        return value == null ? null : value;
    }

    private String normalizeCategory(String category) {
        String value = normalizeBlankToNull(category);
        return PRIMARY_CATEGORIES.contains(value) ? value : "기타";
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Integer parseIntegerOrNull(String value) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }
        return new BigDecimal(normalized.replace(",", "")).intValue();
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = normalizeBlankToNull(value);
        return normalized == null ? null : new BigDecimal(normalized.replace(",", ""));
    }

    private boolean hasAnyNutrition(BigDecimal calories, BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat) {
        return calories != null || carbohydrate != null || protein != null || fat != null;
    }

    @FunctionalInterface
    private interface HeaderValueReader {
        String read(String header);
    }
}
