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
        String path = request.getServletPath();
        if ("/signup".equals(path)) {
            return buildView("login/signup", e.getMessage());
        }
        if ("/forgot-password".equals(path)) {
            String message = e.getMessage();
            ErrorCode code = e.getErrorCode();
            if (EnumSet.of(ErrorCode.USER_NOT_FOUND, ErrorCode.INVALID_EMAIL, ErrorCode.INVALID_USERNAME).contains(code)) {
                message = "아이디 또는 이메일이 올바르지 않습니다.";
            }
            return buildView("login/forgotPassword", message);
        }
        if ("/reset-password".equals(path)) {
            return buildView("login/resetPassword", e.getMessage());
        }
        return buildView("login/login", e.getMessage());
    }

    private ModelAndView buildView(String viewName, String message) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.addObject("error", message);
        return modelAndView;
    }
}