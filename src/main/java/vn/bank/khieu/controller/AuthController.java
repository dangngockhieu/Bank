package vn.bank.khieu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.auth.LoginDTO;
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

        @Value("${refresh-token-validity-in-seconds}")
        private long refreshTokenExpired;

        @PostMapping("/login")
        @ApiMessage("Đăng nhập bằng email và mật khẩu")
        public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
                // Nạp input vào Spring Security để xác thực
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                loginDTO.getEmail(), loginDTO.getPassword());

                // Xác thực (Sẽ gọi UserDetailsCustom để load user từ DB)
                Authentication authentication = authenticationManagerBuilder.getObject()
                                .authenticate(authenticationToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                ResLoginDTO.UserInfo userInfo = new ResLoginDTO.UserInfo(
                                principal.getId(),
                                principal.getEmail(),
                                principal.getFullName(),
                                principal.isActive(),
                                principal.getRole());

                ResLoginDTO res = new ResLoginDTO();
                res.setUser(userInfo);

                // Tạo Access Token
                String accessToken = this.securityUtil.createAccessToken(loginDTO.getEmail(), res.getUser());
                res.setAccessToken(accessToken);

                // Tạo Refresh Token (Chỉ chứa Email và UserId)
                String refreshToken = this.securityUtil.createRefreshToken(loginDTO.getEmail(), principal.getId());

                // LƯU VÀO DATABASE thông qua AuthService
                this.authService.updateUserToken(loginDTO.getEmail(), refreshToken);

                // Thiết lập Cookie để trả về cho trình duyệt
                ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                                .httpOnly(true)
                                .secure(false)
                                .path("/")
                                // .maxAge(refreshTokenExpired)
                                .sameSite("Strict")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .body(res);
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
        @ApiMessage("Thu hồi token")
        public ResponseEntity<Void> revokeToken() {
                String email = SecurityUtil.getCurrentUserLogin().orElse(null);
                if (email != null) {
                        this.authService.revokeToken(email);
                }
                return ResponseEntity.ok().build();
        }

        @PostMapping("/refresh")
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
}
