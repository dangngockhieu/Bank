package vn.bank.khieu.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.entity.UserSession;
import vn.bank.khieu.repository.UserRepository;
import vn.bank.khieu.repository.UserSessionRepository;
import vn.bank.khieu.utils.SecurityUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    @Value("${refresh-token-validity-in-seconds}")
    private long refreshTokenExpired;

    public Optional<User> findByEmailAndRefreshToken(String email, String refreshToken) {
        String hashedToken = SecurityUtil.hashWithSHA256(refreshToken);
        return userRepository.findByEmailAndRefreshToken(email, hashedToken);
    }

    @Transactional
    public void updateUserToken(String email, String refreshToken) {
        // Tìm User từ email định danh
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        UserSession session = this.userSessionRepository.findByUserId(user.getId())
                .orElse(new UserSession());
        session.setUser(user);
        if (refreshToken == null) {
            session.setRefreshToken(null);
        } else {
            String hashedToken = SecurityUtil.hashWithSHA256(refreshToken);
            session.setRefreshToken(hashedToken);
        }
        session.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpired));
        session.setRevoked(false);

        this.userSessionRepository.save(session);
    }

    public void revokeToken(String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        UserSession session = this.userSessionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đăng nhập cho người dùng: " + email));

        session.setRevoked(true);
        this.userSessionRepository.save(session);
    }
}
