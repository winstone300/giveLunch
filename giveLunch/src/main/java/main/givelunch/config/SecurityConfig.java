package main.givelunch.config;

import lombok.RequiredArgsConstructor;
import main.givelunch.model.Role;
import main.givelunch.properties.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    private final SecurityProperties securityProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] permitAdmin = securityProperties.permitAdmin().toArray(new String[0]);
        String[] permitUser = securityProperties.permitAllUser().toArray(new String[0]);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitAdmin).hasAuthority(Role.ADMIN.value())
                        .requestMatchers(permitUser).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")    // GET /login -> 내가 만든 페이지
                        .loginProcessingUrl("/login")   // POST /login -> 시큐리티가 처리(중요)
                        .usernameParameter("userName")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals(Role.ADMIN.value()));
                            if (isAdmin) {
                                response.sendRedirect("/admin");      // 관리자 성공 URL
                            } else {
                                response.sendRedirect("/roulette");   // 일반 성공 URL
                            }
                        })
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/roulette")
                );

        return http.build();
    }
}
