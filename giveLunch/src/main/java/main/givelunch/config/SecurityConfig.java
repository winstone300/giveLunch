package main.givelunch.config;

import lombok.RequiredArgsConstructor;
import main.givelunch.model.Role;
import main.givelunch.properties.AgentAuthProperties;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.security.agent.AgentAccessDeniedHandler;
import main.givelunch.security.agent.AgentApiAuthFilter;
import main.givelunch.security.agent.AgentAuthenticationEntryPoint;
import main.givelunch.security.agent.FixedApiKeyAgentAuthService;
import main.givelunch.services.login.LoginAttemptService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.LockedException;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({SecurityProperties.class, AgentAuthProperties.class})
public class SecurityConfig {
    private final SecurityProperties securityProperties;
    private final LoginAttemptService loginAttemptService;
    private final AgentApiAuthFilter agentApiAuthFilter;
    private final AgentAuthenticationEntryPoint agentAuthenticationEntryPoint;
    private final AgentAccessDeniedHandler agentAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] permitAdmin = securityProperties.permitAdmin().toArray(new String[0]);
        String[] permitUser = securityProperties.permitAllUser().toArray(new String[0]);

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/agent/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/agent/**").hasAuthority(FixedApiKeyAgentAuthService.AGENT_API_ROLE)
                        .requestMatchers(permitUser).permitAll()
                        .requestMatchers(permitAdmin).hasAuthority(Role.ADMIN.value())
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(agentAuthenticationEntryPoint, request -> request.getRequestURI().startsWith("/api/agent/"))
                        .defaultAccessDeniedHandlerFor(agentAccessDeniedHandler, request -> request.getRequestURI().startsWith("/api/agent/"))
                )
                .formLogin(form -> form
                        .loginPage("/login")    // GET /login -> 내가 만든 페이지로 이동
                        .loginProcessingUrl("/login")   // POST /login -> 시큐리티가 처리
                        .usernameParameter("userName")
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.onLoginSuccess(authentication.getName());
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals(Role.ADMIN.value()));
                            if (isAdmin) {
                                response.sendRedirect("/admin");      // 관리자 성공 URL
                            } else {
                                response.sendRedirect("/roulette");   // 일반 성공 URL
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            String userName = request.getParameter("userName");
                            boolean fromRoulette = "roulette".equals(request.getParameter("loginSource"));
                            boolean isLocked = exception instanceof LockedException;
                            long remainingSeconds = 0;
                            if (userName != null && !userName.isBlank()) {
                                isLocked = isLocked || loginAttemptService.onLoginFailure(userName);
                                if (isLocked) {
                                    remainingSeconds = loginAttemptService.getRemainingLockSeconds(userName);
                                }
                            }
                            if (isLocked) {
                                if (fromRoulette) {
                                    response.sendRedirect("/roulette?loginLocked=true&remainingSeconds=" + Math.max(remainingSeconds, 0));
                                    return;
                                }
                                response.sendRedirect("/login?locked=true&remainingSeconds=" + Math.max(remainingSeconds, 0));
                                return;
                            }
                            if (fromRoulette) {
                                response.sendRedirect("/roulette?loginError=true");
                                return;
                            }
                            response.sendRedirect("/login?error=true");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/roulette")
                );

        http.addFilterBefore(agentApiAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
