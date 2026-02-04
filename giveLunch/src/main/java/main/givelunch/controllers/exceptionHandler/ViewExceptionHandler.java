package main.givelunch.controllers.exceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import main.givelunch.controllers.LoginController;
import main.givelunch.controllers.PasswordResetController;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {LoginController.class, PasswordResetController.class})
public class ViewExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ModelAndView handleValidationException(ValidationException e, HttpServletRequest request) {
        String path = normalizePath(request);
        if (path.endsWith("/signup")) {
            return buildView("login/signup", e.getMessage());
        }
        if (path.endsWith("/forgot-password")) {
            String message = e.getMessage();
            ErrorCode code = e.getErrorCode();
            if (EnumSet.of(ErrorCode.USER_NOT_FOUND, ErrorCode.INVALID_EMAIL, ErrorCode.INVALID_USERNAME).contains(code)) {
                message = "아이디 또는 이메일이 올바르지 않습니다.";
            }
            return buildView("login/forgotPassword", message);
        }
        if (path.endsWith("/reset-password")) {
            return buildView("login/resetPassword", e.getMessage());
        }
        return buildView("login/login", e.getMessage());
    }

    private ModelAndView buildView(String viewName, String message) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.addObject("error", message);
        return modelAndView;
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/".equals(path)) {
            return "/";
        }
        if (path == null || path.isBlank()) {
            return "";
        }

        path = path.replaceAll("/+$", "");
        return path;
    }
}