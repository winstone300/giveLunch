package main.givelunch.services.roulette;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.MenuDto;
import main.givelunch.entities.Menu;
import main.givelunch.repositories.MenuRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;

    private List<MenuDto> loadMenu(String userName) {
        List<Menu> entities = menuRepository.findByUserName(userName);
        return entities.stream()
                .map(MenuDto::new)
                .collect(Collectors.toList());
    }

    public List<String> loadMenuToString(String userName) {
        List<MenuDto> menuDtoList = loadMenu(userName);

        if (menuDtoList.isEmpty()) {
            return List.of("김치찌개", "제육볶음", "돈까스"); // 기본값 제공
        }

        List<String> menuStringList = menuDtoList.stream()
                .map(MenuDto::getMenuName)
                .collect(Collectors.toList());

        return menuStringList;
    }

    public void saveMenu(String userName, String menuName) {
        Menu menu = Menu.createMenu(userName,menuName);
        menuRepository.save(menu);
    }

    public void deleteMenu(String userName, String menuName) {
        menuRepository.deleteByUserNameAndMenuName(userName,menuName);
    }
}
