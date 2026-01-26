package main.givelunch.services.roulette;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import main.givelunch.properties.MenuProperties;
import main.givelunch.dto.MenuDto;
import main.givelunch.entities.Menu;
import main.givelunch.repositories.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuProperties menuProperties;

    // 메뉴 리스트 String 반환(비어 있으면 기본 값으로 설정)
    public List<String> loadMenuToString(String userName) {
        List<Menu> menus = menuRepository.findByUserName(userName);

        if (menus.isEmpty()) {
            return menuProperties.defaults();
        }

        return menus.stream()
                .map(Menu::getMenuName)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveMenu(String userName, String menuName) {
        Menu menu = Menu.of(userName,menuName);
        menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(String userName, String menuName) {
        menuRepository.deleteByUserNameAndMenuName(userName,menuName);
    }
}
