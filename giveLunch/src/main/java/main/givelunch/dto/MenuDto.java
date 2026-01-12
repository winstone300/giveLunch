package main.givelunch.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.givelunch.entities.Menu;

@Getter
@Setter
@NoArgsConstructor
public class MenuDto {
    private String userName;
    private String menuName;

    public MenuDto(Menu menu) {
        this.userName = menu.getUserName();
        this.menuName = menu.getMenuName();
    }
}
