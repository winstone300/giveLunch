package main.givelunch.repositories;

import java.util.List;
import main.givelunch.dto.MenuDto;
import main.givelunch.entities.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByUserName(String userName);
    void deleteByUserNameAndMenuName(String userName, String menuName);
}
