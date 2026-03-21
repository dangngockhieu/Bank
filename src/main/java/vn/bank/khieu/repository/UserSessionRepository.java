package vn.bank.khieu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    void deleteByUserId(Long userId);

    Optional<UserSession> findByRefreshTokenAndRevokedFalse(String refreshToken);

    Optional<UserSession> findByUserId(Long userId);
}
