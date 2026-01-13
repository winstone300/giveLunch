package main.givelunch.repositories;

import java.util.List;
import java.util.Optional;
import main.givelunch.entities.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findTop10ByNameContaining(String keyword);
    Long findIdByNameContaining(String keyword);
}
