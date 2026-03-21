package vn.bank.khieu.config;

import java.io.IOException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.utils.SecurityUtil;

@Component
@RequiredArgsConstructor
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy email từ SecurityContext
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);

        if (email != null) {
            // Check "Kill Switch" trên Redis
            String isBlocked = stringRedisTemplate.opsForValue().get("blacklist:user:" + email);

            if (isBlocked != null) {
                // Nếu bị khóa, trả về 401 ngay lập tức
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"Tài khoản đã bị thu hồi quyền truy cập khẩn cấp\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
