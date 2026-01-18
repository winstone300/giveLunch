package main.givelunch.services.roulette;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;
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
    @DisplayName("loadMenuToString: 메뉴가 비어있으면 default 메뉴 리스트 반환")
    void loadMenuToStringReturnsDefaultsWhenNoMenus() {
        // given
        String userName = "tester";

        //when
        List<String> menus = menuService.loadMenuToString(userName);

        //then
        assertThat(menus).containsExactlyElementsOf(menuProperties.defaults());
    }

    @Test
    @DisplayName("saveMenu/loadMenuToString: 사용자 메뉴 저장 후 조회 시 저장된 메뉴를 반환")
    void saveMenuPersistsAndLoadsMenusForUser() {
        //given
        menuService.saveMenu("tester", "비빔밥");
        menuService.saveMenu("tester", "칼국수");

        //when
        List<String> menus = menuService.loadMenuToString("tester");

        //then
        assertThat(menus).containsExactlyInAnyOrder("비빔밥", "칼국수");
    }
}