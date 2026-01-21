package main.givelunch.controllers;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.SignupRequestDto;
import main.givelunch.services.login.SignupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final SignupService signupService;

    @GetMapping("/login")
    public String login() {
        return "login/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "login/signup";
    }

    // 상태코드 추가 필요
    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupRequestDto req, Model model) {
        try {
            signupService.signup(req);
            return "redirect:/login?success";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "login/signup";
        }
    }
}
