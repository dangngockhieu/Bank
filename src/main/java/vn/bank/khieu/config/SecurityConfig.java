package vn.bank.khieu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

        private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        private final JwtAuthenticationConverter jwtAuthenticationConverter;
        private final JwtBlacklistFilter jwtBlacklistFilter;

        public SecurityConfig(CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                        JwtAuthenticationConverter jwtAuthenticationConverter, JwtBlacklistFilter jwtBlacklistFilter) {
                this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
                this.jwtAuthenticationConverter = jwtAuthenticationConverter;
                this.jwtBlacklistFilter = jwtBlacklistFilter;
        }

        @Bean
        // Config PasswordEncoder
        public PasswordEncoder passwordEncoder() {
                return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        }

        @Bean
        public SecurityFilterChain filterChain(
                        HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .authorizeHttpRequests(
                                                authz -> authz
                                                                // API PUBLIC
                                                                .requestMatchers("/api/auth/login",
                                                                                "/api/auth/refresh-token")
                                                                .permitAll()
                                                                .requestMatchers("/api/auth/send-reset-password-email",
                                                                                "/api/auth/reset-password")
                                                                .permitAll()
                                                                .requestMatchers("/", "/storage/**", "/v3/api-docs/**",
                                                                                "/swagger-ui/**")
                                                                .permitAll()

                                                                .anyRequest().authenticated())
                                .addFilterAfter(jwtBlacklistFilter, BearerTokenAuthenticationFilter.class)
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt
                                                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                                                .authenticationEntryPoint(customAuthenticationEntryPoint))
                                .exceptionHandling(
                                                exceptions -> exceptions
                                                                .authenticationEntryPoint(
                                                                                new BearerTokenAuthenticationEntryPoint()) // 401
                                                                .accessDeniedHandler(
                                                                                new BearerTokenAccessDeniedHandler())) // 403
                                .formLogin(f -> f.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                return http.build();
        }
}
