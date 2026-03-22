package vn.bank.khieu.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.EmailDTO;
import vn.bank.khieu.dto.request.auth.LoginDTO;
import vn.bank.khieu.dto.request.auth.ResetPasswordDTO;
import vn.bank.khieu.dto.response.ResStringDTO;
import vn.bank.khieu.dto.response.auth.ResLoginDTO;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.service.AuthService;
import vn.bank.khieu.service.UserService;
import vn.bank.khieu.service.user.UserPrincipal;
import vn.bank.khieu.utils.SecurityUtil;
import vn.bank.khieu.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthenticationManagerBuilder authenticationManagerBuilder;
        private final SecurityUtil securityUtil;
        private final AuthService authService;
        private final UserService userService;
        private final StringRedisTemplate stringRedisTemplate;

        @Value("${refresh-token-validity-in-seconds}")
        private long refreshTokenExpired;

        @PostMapping("/login")
        @ApiMessage("Đăng nhập bằng email và mật khẩu")
        public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
                String email = loginDTO.getEmail();
                String loginFailKey = "bank:auth:fail_count:login:" + email;

                // KIỂM TRA XEM CÓ ĐANG BỊ KHÓA DO NHẬP SAI QUÁ NHIỀU KHÔNG
                String failCountStr = stringRedisTemplate.opsForValue().get(loginFailKey);
                if (failCountStr != null && Integer.parseInt(failCountStr) >= 5) {
                        Long remain = stringRedisTemplate.getExpire(loginFailKey, TimeUnit.MINUTES);
                        throw new RuntimeException("Tài khoản bị khóa. Vui lòng thử lại sau "
                                        + (remain != null ? remain : 0) + " phút.");
                }

                try {
                        // Nạp input vào Spring Security để xác thực
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                        email, loginDTO.getPassword());

                        Authentication authentication = authenticationManagerBuilder.getObject()
                                        .authenticate(authenticationToken);

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Nếu Login success Xóa biến đếm sai
                        stringRedisTemplate.delete(loginFailKey);

                        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                        ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                                        principal.getId(),
                                        principal.getEmail(),
                                        principal.getFullName(),
                                        principal.isActive(),
                                        principal.getRole());

                        ResLoginDTO res = new ResLoginDTO();
                        res.setUser(userInfo);

                        String accessToken = this.securityUtil.createAccessToken(email, res.getUser());
                        res.setAccessToken(accessToken);
                        String refreshToken = this.securityUtil.createRefreshToken(email, principal.getId());

                        this.authService.updateUserToken(email, refreshToken);

                        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                                        .httpOnly(true)
                                        .secure(false)
                                        .path("/")
                                        .maxAge(refreshTokenExpired)
                                        .sameSite("Strict")
                                        .build();

                        return ResponseEntity.ok()
                                        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                        .body(res);

                } catch (BadCredentialsException e) {
                        // NẾU ĐĂNG NHẬP THẤT BẠI
                        Long currentFail = stringRedisTemplate.opsForValue().increment(loginFailKey);

                        if (currentFail != null && currentFail == 1) {
                                stringRedisTemplate.expire(loginFailKey, 30, TimeUnit.MINUTES);
                        }

                        if (currentFail != null && currentFail >= 5) {
                                stringRedisTemplate.expire(loginFailKey, 30, TimeUnit.MINUTES);
                                throw new RuntimeException(
                                                "Bạn đã nhập sai 5 lần. Tài khoản bị khóa đăng nhập trong 30 phút.");
                        }

                        throw new RuntimeException(
                                        "Mật khẩu không chính xác. Bạn còn " + (5 - currentFail) + " lần thử.");
                }
        }

        @GetMapping("/account")
        @ApiMessage("Lấy thông tin tài khoản")
        public ResponseEntity<ResLoginDTO.UserInfo> getAccount() {
                String email = SecurityUtil.getCurrentUserLogin().orElse(null);
                User user = this.userService.findUserByEmail(email)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy User với email: " + email));
                ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                                user.getId(),
                                user.getEmail(),
                                user.getFullName(),
                                user.isActive(),
                                user.getRole().getName().name());

                return ResponseEntity.ok(userInfo);
        }

        @PostMapping("/logout")
        @ApiMessage("Đăng xuất, xóa token và cookie")
        public ResponseEntity<Void> logout() {
                String email = SecurityUtil.getCurrentUserLogin().orElse(null);
                if (email != null) {
                        this.authService.updateUserToken(email, null);
                }

                // Xóa cookie Refresh Token
                ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(false)
                                .path("/")
                                .maxAge(0)
                                .sameSite("Strict")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                                .build();
        }

        @PostMapping("/revoke")
        @ApiMessage("Nhân viên thu hồi quyền truy cập của khách hàng")
        public ResponseEntity<ResStringDTO> revokeToken(@Valid @RequestBody EmailDTO emailDTO) {
                this.authService.revokeToken(emailDTO.getEmail());
                return ResponseEntity.ok()
                                .body(new ResStringDTO("Đã thu hồi quyền truy cập của " + emailDTO.getEmail()));
        }

        @PostMapping("/refresh-token")
        @ApiMessage("Làm mới token")
        public ResponseEntity<ResLoginDTO> refreshAccessToken(@CookieValue(name = "refreshToken") String refreshToken) {
                if (refreshToken == null || refreshToken.isEmpty())
                        throw new RuntimeException("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
                Jwt decodedToken = this.securityUtil.checkValidToken(refreshToken);
                String email = decodedToken.getSubject();

                User user = this.authService.findByEmailAndRefreshToken(email, refreshToken)
                                .orElseThrow(() -> new RuntimeException(
                                                "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"));
                ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                                user.getId(),
                                user.getEmail(),
                                user.getFullName(),
                                user.isActive(),
                                user.getRole().getName().name());
                ResLoginDTO res = new ResLoginDTO();
                res.setUser(userInfo);
                String accessToken = this.securityUtil.createAccessToken(email, res.getUser());
                res.setAccessToken(accessToken);
                return ResponseEntity.ok(res);
        }

        @PostMapping("/send-reset-password-email")
        @ApiMessage("Gửi email đặt lại mật khẩu")
        public ResponseEntity<ResStringDTO> sendResetPasswordEmail(@Valid @RequestBody EmailDTO emailDTO) {
                this.authService.sendEmailResetPassword(emailDTO.getEmail());
                return ResponseEntity.ok().body(new ResStringDTO("Email đặt lại mật khẩu đã được gửi"));
        }

        @PatchMapping("/reset-password")
        @ApiMessage("Đặt lại mật khẩu")
        public ResponseEntity<ResStringDTO> resetPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO) {
                this.authService.resetPassword(resetPasswordDTO);
                return ResponseEntity.ok().body(new ResStringDTO("Mật khẩu đã được đặt lại thành công"));
        }
}
