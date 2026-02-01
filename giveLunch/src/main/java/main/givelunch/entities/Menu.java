package main.givelunch.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(name="menus",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_menus_user_menu", columnNames = {"user_name", "menu_name"})
        })
@Getter
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_name")
    private String userName;

    @Column(name = "menu_name")
    private String menuName;

    @Column(name = "food_id")
    private Long foodId;

    public static Menu of(String userName,String menuName,Long foodId){
        Menu menu = new Menu();
        menu.userName = userName;
        menu.menuName = menuName;
        menu.foodId = foodId;
        return menu;
    }
}
