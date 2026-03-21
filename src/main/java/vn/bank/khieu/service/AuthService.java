package vn.bank.khieu.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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

    public Optional<User> findByEmailAndRefreshToken(String email, String refreshToken) {
        // 1. Tìm session trong DB dựa trên token
        return userSessionRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .filter(session -> session.getUser().getEmail().equals(email))
                .filter(session -> session.getExpiresAt().isAfter(java.time.Instant.now()))
                .map(UserSession::getUser);
    }

    @Transactional
    public void updateUserToken(String email, String refreshToken) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        stringRedisTemplate.delete("session:" + email);

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
                    "session:" + email,
                    hashedToken,
                    refreshTokenExpired,
                    TimeUnit.SECONDS);
        }

        userSessionRepository.save(session);
    }

    public void revokeToken(String email) {
        // Khóa trong DB
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        UserSession session = this.userSessionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Session không tồn tại"));
        session.setRevoked(true);
        this.userSessionRepository.save(session);

        // Mọi request của email này sau đó sẽ bị Filter kiểm tra và chặn lại
        stringRedisTemplate.opsForValue().set("blacklist:user:" + email, "true", accessTokenExpired, TimeUnit.HOURS);

        // Xóa session cache
        stringRedisTemplate.delete("session:" + email);
    }

    public void sendEmailResetPassword(String email) {
        if (email == null)
            throw new BadCredentialsException("Email là bắt buộc");

        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trên hệ thống"));

        String lockKey = "otp:lock:reset_password:" + email;
        String dailyCountKey = "fail_count:reset_password:" + email + ":" + java.time.LocalDate.now();

        // CHỐNG SPAM GỬI LIÊN TỤC (Khóa 5 phút)
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            Long remainSeconds = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            long minutes = remainSeconds / 60;
            long seconds = remainSeconds % 60;
            throw new RuntimeException(
                    String.format("Vui lòng đợi %d phút %d giây để yêu cầu mã mới.", minutes, seconds));
        }

        // --- GIỚI HẠN TỔNG LẦN GỬI TRONG NGÀY (Tối đa 5 lần) ---
        String currentCount = stringRedisTemplate.opsForValue().get(dailyCountKey);
        if (currentCount != null && Integer.parseInt(currentCount) >= 5) {
            throw new RuntimeException("Bạn đã vượt quá hạn mức nhận OTP trong hôm nay (Tối đa 5 lần/ngày).");
        }

        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));

        // LƯU REDIS
        // Lưu mã xác thực (hạn 5 phút)
        stringRedisTemplate.opsForValue().set("reset_token:" + email, otpCode, 5, TimeUnit.MINUTES);

        // TẠO KHÓA CHẶN GỬI LẠI
        stringRedisTemplate.opsForValue().set(lockKey, "true", 5, TimeUnit.MINUTES);

        // Tăng số lần gửi trong ngày
        stringRedisTemplate.opsForValue().increment(dailyCountKey);
        stringRedisTemplate.expire(dailyCountKey, 24, TimeUnit.HOURS);

        // GỬI MAIL
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", user.getFullName());
        variables.put("codeID", otpCode);

        emailService.sendEmailFromTemplate(
                user.getEmail(),
                "K-Bank: Xác thực đặt lại mật khẩu",
                "passwordReset",
                variables);
    }

    public void resetPassword(ResetPasswordDTO dto) {
        // Kiểm tra mật khẩu khớp nhau
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        String email = dto.getEmail();
        String failCountKey = "fail_count:reset_password:" + email;

        // KIỂM TRA SỐ LẦN NHẬP SAI ---
        String failCountStr = stringRedisTemplate.opsForValue().get(failCountKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= 5) {
            Long remainSeconds = stringRedisTemplate.getExpire(failCountKey, TimeUnit.SECONDS);
            long minutes = remainSeconds / 60;
            throw new IllegalArgumentException(
                    String.format("Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau %d phút.", minutes));
        }

        // Lấy mã OTP từ Redis
        String otpKey = "reset_token:" + email;
        String savedOtp = stringRedisTemplate.opsForValue().get(otpKey);

        if (savedOtp == null) {
            throw new IllegalArgumentException("Mã xác thực đã hết hạn hoặc không tồn tại");
        }

        // KIỂM TRA MÃ OTP
        if (!savedOtp.equals(dto.getCode())) {
            // Tăng biến đếm sai
            Long currentFail = stringRedisTemplate.opsForValue().increment(failCountKey);
            // Nếu là lần đầu tiên sai, đặt thời gian khóa là 30 phút
            if (currentFail != null && currentFail == 1) {
                stringRedisTemplate.expire(failCountKey, 30, TimeUnit.MINUTES);
            }

            if (currentFail != null && currentFail >= 5) {
                // Nếu đúng lần thứ 5 sai, đảm bảo key sống đủ 30 phút kể từ lúc bị block
                stringRedisTemplate.expire(failCountKey, 30, TimeUnit.MINUTES);
                throw new IllegalArgumentException(
                        "Bạn đã nhập sai 5 lần. Tài khoản bị tạm khóa chức năng này trong 30 phút.");
            }

            throw new IllegalArgumentException(
                    "Mã xác thực không chính xác. Bạn còn " + (5 - currentFail) + " lần thử.");
        }

        // NẾU ĐÚNG: CẬP NHẬT MẬT KHẨU
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.userRepository.save(user);

        // DỌN DẸP REDIS
        stringRedisTemplate.delete(otpKey); // Xóa mã OTP
        stringRedisTemplate.delete(failCountKey); // Xóa biến đếm sai
        stringRedisTemplate.delete("otp:lock:reset_password:" + email); // Mở khóa gửi mail

        // Đăng xuất tất cả thiết bị
        this.revokeToken(email);
    }
}
