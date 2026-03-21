package vn.bank.khieu.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.auth.ResetPasswordDTO;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${refresh-token-validity-in-seconds}")
    private long refreshTokenExpired;

    @Value("${access-token-validity-in-seconds}")
    private long accessTokenExpired;

    @Transactional(readOnly = true)
    public Optional<User> findByEmailAndRefreshToken(String email, String refreshToken) {
        String hashed = SecurityUtil.hashWithSHA256(refreshToken);
        return userSessionRepository.findByRefreshTokenAndRevokedFalse(hashed)
                .filter(session -> session.getUser().getEmail().equals(email))
                .filter(session -> session.getExpiresAt().isAfter(java.time.Instant.now()))
                .map(UserSession::getUser);
    }

    @Transactional
    public void updateUserToken(String email, String refreshToken) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        stringRedisTemplate.delete("bank:session:" + email);

        UserSession session = userSessionRepository.findByUserId(user.getId()).orElse(new UserSession());
        session.setUser(user);

        if (refreshToken == null) {
            session.setRefreshToken(null);
            session.setRevoked(true);
            session.setExpiresAt(Instant.now());
        } else {
            String hashedToken = SecurityUtil.hashWithSHA256(refreshToken);
            session.setRefreshToken(hashedToken);
            session.setRevoked(false);
            session.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpired));

            stringRedisTemplate.opsForValue().set(
                    "bank:session:" + email,
                    hashedToken,
                    refreshTokenExpired,
                    TimeUnit.SECONDS);
        }

        userSessionRepository.save(session);
    }

    @Transactional
    public void revokeToken(String email) {
        // Khóa trong DB
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        userSessionRepository.findByUserId(user.getId()).ifPresent(session -> {
            session.setRevoked(true);
            userSessionRepository.save(session);
        });

        // Mọi request của email này sau đó sẽ bị Filter kiểm tra và chặn lại
        stringRedisTemplate.opsForValue().set("bank:blacklist:user:" + email, "true", accessTokenExpired,
                TimeUnit.SECONDS);

        // Xóa session cache
        stringRedisTemplate.delete("bank:session:" + email);
    }

    public void sendEmailResetPassword(String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        String lockKey = "bank:otp:lock:reset_password:" + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            Long remain = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            throw new RuntimeException("Vui lòng đợi " + (remain != null ? remain : 0) + " giây để yêu cầu mã mới.");
        }

        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));
        stringRedisTemplate.opsForValue().set("reset_token:" + email, otpCode, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(lockKey, "true", 5, TimeUnit.MINUTES);

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", user.getFullName());
        variables.put("codeID", otpCode);

        emailService.sendEmailFromTemplate(user.getEmail(), "Bank: Đặt lại mật khẩu", "passwordReset", variables);
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        String email = dto.getEmail();
        String otpKey = "reset_token:" + email;
        String savedOtp = stringRedisTemplate.opsForValue().get(otpKey);

        if (savedOtp == null || !savedOtp.equals(dto.getCode())) {
            throw new IllegalArgumentException("Mã xác thực không chính xác hoặc đã hết hạn");
        }

        stringRedisTemplate.delete(otpKey);

        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // Dọn dẹp
        stringRedisTemplate.delete("bank:otp:lock:reset_password:" + email);
        stringRedisTemplate.delete("bank:fail_count:reset_password:" + email);

        // Gọi revoke để kích hoạt Blacklist trên Redis
        this.revokeToken(email);
    }
}
