package main.givelunch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodSuggestionDto;
import main.givelunch.dto.MenuDto;
import main.givelunch.services.roulette.FoodSearchService;
import main.givelunch.services.roulette.MenuService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequiredArgsConstructor
public class RouletteController {
    private final MenuService menuService;
    private final FoodSearchService foodSearchService;

    @GetMapping({"/","/roulette"})
    public String roulette(Principal principal, Model model) {
        boolean isLoggedIn = (principal != null);
        String userName = isLoggedIn ? principal.getName() : "GUEST";
        List<String> menuList = menuService.loadMenuToString(userName);

        model.addAttribute("menuList", menuList);
        model.addAttribute("userName", userName);
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "roulette/roulette";
    }

    @Operation(summary = "메뉴 추가", description = "사용자 메뉴 목록에 항목을 추가")
    @PostMapping("/api/menus")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void addMenu(@RequestBody MenuDto menuDto, Principal principal) {
        menuService.saveMenu(principal.getName(), menuDto.menuName());
    }

    // 메뉴 삭제 API
    @Operation(summary = "메뉴 삭제", description = "사용자 메뉴 목록에서 항목을 삭제")
    @DeleteMapping("/api/menus")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteMenu(@RequestBody MenuDto menuDto, Principal principal) {
        menuService.deleteMenu(principal.getName(), menuDto.menuName());
    }

    @Operation(summary = "메뉴 자동완성", description = "사용자 메뉴 목록에서 이름을 검색")
    @GetMapping("/api/menus/suggest")
    @ResponseBody
    public List<FoodSuggestionDto> suggestFoods(@RequestParam("query") String query) {
        return foodSearchService.suggestFoods(query);
    }
}
