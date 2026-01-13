package main.givelunch.repositories;

import java.util.Optional;
import main.givelunch.entities.Nutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionRepository extends JpaRepository<Nutrition, Long> {
    Optional<Nutrition> findByFoodId(Long foodId);
}
