package main.givelunch.controllers;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.loginDto.SignupRequestDto;
import main.givelunch.services.login.SignupService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final SignupService signupService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", defaultValue = "false") boolean error,
                        @RequestParam(value = "locked", defaultValue = "false") boolean locked,
                        @RequestParam(value = "remainingSeconds", defaultValue = "0") long remainingSeconds,
                        @RequestParam(value = "success", defaultValue = "false") boolean success,
                        @RequestParam(value = "resetSuccess", defaultValue = "false") boolean resetSuccess) {
        if (locked) {
            return "redirect:/roulette?loginLocked=true&remainingSeconds=" + Math.max(remainingSeconds, 0);
        }
        if (error) {
            return "redirect:/roulette?loginError=true";
        }
        if (success) {
            return "redirect:/roulette?loginSuccess=true";
        }
        if (resetSuccess) {
            return "redirect:/roulette?resetSuccess=true";
        }
        return "redirect:/roulette?loginModalOpen=true";
    }

    @GetMapping("/signup")
    public String signup() {
        return "login/signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupRequestDto req) {
        signupService.signup(req);
        return "redirect:/roulette?loginSuccess=true";
    }
}
