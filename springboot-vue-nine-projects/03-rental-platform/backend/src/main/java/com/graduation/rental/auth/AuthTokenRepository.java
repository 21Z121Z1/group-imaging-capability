package com.graduation.rental.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime; import java.util.Optional;
public interface AuthTokenRepository extends JpaRepository<AuthToken,Long> { Optional<AuthToken> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now); void deleteByTokenHash(String tokenHash); long deleteByExpiresAtBefore(LocalDateTime now); }
