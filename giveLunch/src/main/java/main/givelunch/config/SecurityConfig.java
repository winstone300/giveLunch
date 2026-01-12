package main.givelunch.config;

import lombok.RequiredArgsConstructor;
import main.givelunch.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final SecurityProperties securityProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] permit = securityProperties.getPermitAll().toArray(new String[0]);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permit).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")    // GET /login -> 내가 만든 페이지
                        .loginProcessingUrl("/login")   // POST /login -> 시큐리티가 처리(중요)
                        .usernameParameter("userName")
                        .defaultSuccessUrl("/roulette", true) // 성공 시 이동
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
