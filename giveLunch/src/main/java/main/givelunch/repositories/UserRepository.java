package main.givelunch.repositories;

import java.math.BigInteger;
import java.util.Optional;
import main.givelunch.entities.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserInfo, BigInteger> {
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    Optional<UserInfo> findByUserName(String userName);
}
