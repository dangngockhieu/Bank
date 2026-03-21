package vn.bank.khieu.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.repository.UserRepository;
import vn.bank.khieu.service.EmailService;

@Service
@RequiredArgsConstructor
public class OtpUtil {
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public void sendEmailOTP(String emailRequest) {
        // Chuẩn hóa email
        String email = emailRequest.toLowerCase().trim();

        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        String lockKey = "bank:otp:lock:transaction:" + email;
        String otpKey = "bank:otp:token:transaction:" + email;

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            Long remain = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            throw new RuntimeException("Vui lòng đợi " + (remain != null ? remain : 0) + " giây để yêu cầu mã mới.");
        }

        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));

        // Lưu vào Redis
        stringRedisTemplate.opsForValue().set(otpKey, otpCode, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(lockKey, "true", 1, TimeUnit.MINUTES);
        // Chuẩn bị dữ liệu gửi Mail
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", user.getFullName());
        variables.put("codeID", otpCode);

        emailService.sendEmailFromTemplate(user.getEmail(), "Bank: Mã xác thực OTP", "OTP", variables);
    }

    public boolean verifyOTP(String email, String code) {
        String otpKey = "bank:otp:token:transaction:" + email;
        String savedOtp = stringRedisTemplate.opsForValue().get(otpKey);

        if (savedOtp != null && savedOtp.equals(code)) {
            // Xóa OTP sau khi xác thực thành công
            stringRedisTemplate.delete(otpKey);
            return true;
        }
        return false;
    }
}
