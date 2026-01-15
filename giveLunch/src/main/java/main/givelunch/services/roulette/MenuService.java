package main.givelunch.services.roulette;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import main.givelunch.properties.MenuProperties;
import main.givelunch.dto.MenuDto;
import main.givelunch.entities.Menu;
import main.givelunch.repositories.MenuRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuProperties menuProperties;

    // 메뉴 리스트 DTO로 반환
    private List<MenuDto> loadMenu(String userName) {
        List<Menu> entities = menuRepository.findByUserName(userName);
        return entities.stream()
                .map(MenuDto::new)
                .collect(Collectors.toList());
    }

    // 메뉴 리스트 String 반환(비어 있으면 기본 값으로 설정)
    public List<String> loadMenuToString(String userName) {
        List<MenuDto> menuDtoList = loadMenu(userName);

        if (menuDtoList.isEmpty()) {
            return menuProperties.defaults();
        }

        List<String> menuStringList = menuDtoList.stream()
                .map(MenuDto::getMenuName)
                .collect(Collectors.toList());

        return menuStringList;
    }

    public void saveMenu(String userName, String menuName) {
        Menu menu = Menu.of(userName,menuName);
        menuRepository.save(menu);
    }

    public void deleteMenu(String userName, String menuName) {
        menuRepository.deleteByUserNameAndMenuName(userName,menuName);
    }
}
