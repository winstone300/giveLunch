package main.givelunch.services.roulette;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import main.givelunch.dto.MenuDto;
import main.givelunch.entities.Menu;
import main.givelunch.properties.MenuProperties;
import main.givelunch.repositories.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock
    private MenuRepository menuRepository;

    @InjectMocks MenuService menuService;

    @Test
    @DisplayName("메뉴리스트 비어있을 때 조회")
    void loadMenu_returnsDefaultsWhenNoMenus() {
        //given
        MenuService menuService =
                new MenuService(menuRepository, new MenuProperties(List.of("치킨","피자")));
        String userName = "user1";
        List<String> defaults = List.of("치킨", "피자");

        when(menuRepository.findByUserName(userName)).thenReturn(Collections.emptyList());

        //when
        List<MenuDto> result = menuService.loadMenu(userName);

        //then
        assertThat(result.stream().map(MenuDto::menuName).collect(Collectors.toList()))
                .containsExactlyElementsOf(defaults);
        assertThat(result).allMatch(menu -> menu.foodId() == null);
    }


    @Test
    @DisplayName("메뉴리스트 조회")
    void loadMenuToString_returnsMenuNamesWhenMenusExist() {
        //given
        String userName = "user1";
        Menu menu1 = Menu.of(userName, "비빔밥", 10L);
        Menu menu2 = Menu.of(userName, "김치찌개", null);

        when(menuRepository.findByUserName(userName)).thenReturn(List.of(menu1, menu2));

        //when
        List<MenuDto> result = menuService.loadMenu(userName);

        //then
        assertThat(result).extracting(MenuDto::menuName)
                .containsExactly("비빔밥", "김치찌개");
        assertThat(result).extracting(MenuDto::foodId)
                .containsExactly(10L, null);
    }

}
