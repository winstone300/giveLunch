package main.givelunch.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import main.givelunch.entities.Menu;
import main.givelunch.properties.MenuProperties;
import main.givelunch.repositories.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RouletteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuProperties menuProperties;

    @Test
    @WithMockUser(username = "tester")
    @DisplayName("GET /roulette: 로그인한 user는 기본 메뉴와 사용자 정보를 확인")
    void roulettePageShowsDefaultsForLoggedInUserWithoutMenus() throws Exception {
        mockMvc.perform(get("/roulette"))
                .andExpect(status().isOk())
                .andExpect(view().name("roulette/roulette"))
                .andExpect(model().attribute("userName", "tester"))
                .andExpect(model().attribute("isLoggedIn", true))
                .andExpect(model().attribute("menuList", menuProperties.defaults()));
    }

    @Test
    @WithMockUser(username = "tester")
    @DisplayName("POST /api/menus: 로그인 user가 메뉴를 추가하면 저장")
    void addMenuPersistsForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/menus")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"비빔밥\"}"))
                .andExpect(status().isOk());

        List<Menu> menus = menuRepository.findByUserName("tester");
        assertThat(menus).hasSize(1);
        assertThat(menus.get(0).getMenuName()).isEqualTo("비빔밥");
    }

    @Test
    @WithMockUser(username = "tester")
    @DisplayName("POST /api/menus/delete: 로그인 user가 메뉴를 삭제하면 제거")
    void deleteMenuRemovesForAuthenticatedUser() throws Exception {
        // given
        menuRepository.save(Menu.of("tester", "칼국수"));

        //when
        mockMvc.perform(post("/api/menus/delete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"칼국수\"}"))
                .andExpect(status().isOk());

        // then
        List<Menu> menus = menuRepository.findByUserName("tester");
        assertThat(menus).isEmpty();
    }
}