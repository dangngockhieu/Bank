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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

        private final JwtAuthenticationConverter jwtAuthenticationConverter;

        SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
                this.jwtAuthenticationConverter = jwtAuthenticationConverter;
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
                                                                // // CÁC API ADMIN
                                                                // .requestMatchers("/api/v1/admin/**")
                                                                // .hasRole("ADMIN")
                                                                // // CÁC API CỦA USER (USER và ADMIN đều được vào)
                                                                // .requestMatchers("/api/v1/user/**")
                                                                // .hasAnyRole("CUSTOMER", "ADMIN")

                                                                // CÁC API Public
                                                                .requestMatchers("/api/auth/**").permitAll()
                                                                .requestMatchers("/api/users/**").permitAll()
                                                                .requestMatchers("/storage/**").permitAll()

                                                                .requestMatchers("/").permitAll()
                                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter)))
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
