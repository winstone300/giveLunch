package main.givelunch.repositories;

import java.math.BigInteger;
import main.givelunch.entities.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserInfo, BigInteger> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
