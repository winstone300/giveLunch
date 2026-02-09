package main.givelunch.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public abstract class VerificationCode {

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column
    private LocalDateTime blockedUntil;

    public boolean isBlocked(LocalDateTime now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }
}