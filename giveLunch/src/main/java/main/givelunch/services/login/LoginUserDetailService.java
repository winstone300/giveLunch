package main.givelunch.services.login;

import lombok.RequiredArgsConstructor;
import main.givelunch.entities.UserInfo;
import main.givelunch.model.Role;
import main.givelunch.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        UserInfo userInfo = userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(userInfo.getUserName())
                .password(userInfo.getPassword())
                .authorities(userInfo.getRole().value())
                .build();
    }
}
