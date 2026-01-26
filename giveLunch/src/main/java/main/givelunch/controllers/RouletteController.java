package main.givelunch.controllers;

import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.MenuDto;
import main.givelunch.services.roulette.MenuService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class RouletteController {
    private final MenuService menuService;

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

    @PostMapping("/api/menus")
    @ResponseBody
    public ResponseEntity<Void> addMenu(@RequestBody Map<String, String> data, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        menuService.saveMenu(principal.getName(), data.get("menuName"));
        return ResponseEntity.noContent().build();
    }

    // 메뉴 삭제 API
    @DeleteMapping("/api/menus/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteMenu(@RequestBody MenuDto menuDto, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        menuService.deleteMenu(principal.getName(), menuDto.getMenuName());
        return ResponseEntity.noContent().build();
    }

}
