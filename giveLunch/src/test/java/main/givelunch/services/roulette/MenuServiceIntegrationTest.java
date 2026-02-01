package main.givelunch.services.roulette;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import main.givelunch.dto.MenuDto;
import main.givelunch.properties.MenuProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MenuServiceIntegrationTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuProperties menuProperties;

    @Test
    @DisplayName("loadMenu: 메뉴가 비어있으면 default 메뉴 리스트 반환")
    void loadMenuReturnsDefaultsWhenNoMenus() {
        // given
        String userName = "tester";

        //when
        List<MenuDto> menus = menuService.loadMenu(userName);

        //then
        List<String> menuNames = menus.stream()
                .map(MenuDto::menuName)
                .collect(Collectors.toList());
        assertThat(menuNames).containsExactlyElementsOf(menuProperties.defaults());
        assertThat(menus).allMatch(menu -> menu.foodId() == null);
    }

    @Test
    @DisplayName("saveMenu/loadMenu: 사용자 메뉴 저장 후 조회 시 저장된 메뉴를 반환")
    void saveMenuPersistsAndLoadsMenusForUser() {
        //given
        menuService.saveMenu("tester", "비빔밥", 101L);
        menuService.saveMenu("tester", "칼국수", null);

        //when
        List<MenuDto> menus = menuService.loadMenu("tester");

        //then
        assertThat(menus).extracting(MenuDto::menuName)
                .containsExactlyInAnyOrder("비빔밥", "칼국수");
        assertThat(menus)
                .filteredOn(menu -> menu.menuName().equals("비빔밥"))
                .extracting(MenuDto::foodId)
                .containsExactly(101L);
        assertThat(menus)
                .filteredOn(menu -> menu.menuName().equals("칼국수"))
                .extracting(MenuDto::foodId)
                .containsExactly((Long) null);
    }
}