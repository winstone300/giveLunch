package main.givelunch.repositories;

import java.util.List;
import java.util.Optional;
import main.givelunch.entities.Food;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    @Query("select f.id from Food f where f.name like %:name% order by length(f.name) asc")
    List<Long> findIdByNameContaining(@Param("name") String name, Pageable pageable);
}
