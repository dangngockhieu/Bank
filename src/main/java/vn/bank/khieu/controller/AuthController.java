package vn.bank.khieu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.auth.LoginDTO;
import vn.bank.khieu.dto.response.auth.ResLoginDTO;
import vn.bank.khieu.service.AuthService;
import vn.bank.khieu.service.user.UserPrincipal;
import vn.bank.khieu.utils.SecurityUtil;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final AuthService authService;

    @Value("${refresh-token-validity-in-seconds}")
    private long refreshTokenExpired;

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // Nạp input vào Spring Security để xác thực
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getEmail(), loginDTO.getPassword());

        // Xác thực (Sẽ gọi UserDetailsCustom để load user từ DB)
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Chuẩn bị dữ liệu Response
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
        String accessToken = this.securityUtil.createAccessToken(loginDTO.getEmail(), res);
        res.setAccessToken(accessToken);

        // Tạo Refresh Token (Chỉ chứa Email và UserId)
        String refreshToken = this.securityUtil.createRefreshToken(loginDTO.getEmail(), principal.getId());

        // LƯU VÀO DATABASE thông qua AuthService (Hàm Khiêu cần viết tiếp)
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
}
