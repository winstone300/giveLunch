package main.givelunch.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name="menus")
@Getter
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_name")
    private String userName;

    @Column(name = "menu_name")
    private String menuName;

    public static Menu of(String userName,String menuName){
        Menu menu = new Menu();
        menu.userName = userName;
        menu.menuName = menuName;
        return menu;
    }
}
