package vn.bank.khieu.service;

import java.time.Instant;

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

    @Transactional
    public void updateUserToken(String email, String refreshToken) {
        // Tìm User từ email định danh
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        // Hash Refresh Token trước khi lưu
        String hashedToken = SecurityUtil.hashWithSHA256(refreshToken);

        UserSession session = this.userSessionRepository.findByUserId(user.getId())
                .orElse(new UserSession());
        session.setUser(user);
        session.setRefreshToken(hashedToken);
        session.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpired));
        session.setRevoked(false);

        this.userSessionRepository.save(session);
    }
}
