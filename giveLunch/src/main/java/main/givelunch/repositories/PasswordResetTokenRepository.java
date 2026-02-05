package main.givelunch.repositories;

import java.util.Optional;
import main.givelunch.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTopByEmailAndCodeOrderByCreatedAtDesc(String email, String code);
    void deleteByEmail(String email);
}