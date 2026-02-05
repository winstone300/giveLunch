package main.givelunch.services.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import main.givelunch.entities.UserInfo;
import main.givelunch.model.Role;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUserDetailService")
class LoginUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginUserDetailService loginUserDetailService;

    @Test
    @DisplayName("loadUserByUsername - 사용자 있으면 UserDetails 반환")
    void loadUserByUsername_returnsUserDetailsWhenUserExists() {
        // given
        UserInfo user = UserInfo.builder()
                .userName("tester")
                .password("encoded-password")
                .role(Role.USER)
                .build();
        when(userRepository.findByUserName("tester")).thenReturn(Optional.of(user));

        // when
        UserDetails result = loginUserDetailService.loadUserByUsername("tester");

        // then
        assertThat(result.getUsername()).isEqualTo("tester");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername - 사용자 없으면 UsernameNotFoundException")
    void loadUserByUsername_throwsWhenUserMissing() {
        // given
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loginUserDetailService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}