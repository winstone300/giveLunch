package main.givelunch.repositories;

import java.time.LocalDateTime;
import java.util.Optional;
import main.givelunch.entities.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndCodeOrderByCreatedAtDesc(String email, String code);
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    boolean existsByEmailAndVerifiedTrueAndExpiresAtAfter(String email, LocalDateTime now);
    void deleteByExpiresAtBefore(LocalDateTime now);
}