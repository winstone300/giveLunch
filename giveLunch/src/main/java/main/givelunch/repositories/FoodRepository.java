package main.givelunch.repositories;

import java.util.List;
import java.util.Optional;
import main.givelunch.entities.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    // 이름으로 foodID 찾을 때(get api/foods/search)
    @Query("select f.id from Food f where f.name = :name")
    Optional<Long> findIdByName(@Param("name") String name);

    // 검색시 음식 추천(get /api/menus/suggest)
    @Query("""
            select f from Food f
            where f.name like concat(:name, '%')
            order by length(f.name) asc
            """)
    List<Food> findByNameContainingOrderByShortestName(@Param("name") String name, Pageable pageable);

    // 관리자 페이지 검색용
    Page<Food> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
