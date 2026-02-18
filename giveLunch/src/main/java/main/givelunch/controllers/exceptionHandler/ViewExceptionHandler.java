package main.givelunch.controllers.exceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import main.givelunch.controllers.LoginController;
import main.givelunch.controllers.PasswordResetController;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {LoginController.class, PasswordResetController.class})
public class ViewExceptionHandler {

    // 폼 바인딩(@ModelAttribute) 단계에서 타입 변환/필드 바인딩 오류가 발생할 때 호출
    @ExceptionHandler(BindException.class)
    public ModelAndView handleBindException(BindException e, HttpServletRequest request) {
        String path = normalizePath(request);
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());

        if (path.endsWith("/signup")) {
            return buildView("login/signup", message);
        }
        if (path.endsWith("/forgot-password")) {
            return buildView("login/forgot-password", "아이디 또는 이메일이 올바르지 않습니다.");
        }
        if (path.endsWith("/reset-password")) {
            return buildView("login/reset-password", message);
        }
        return buildView("login/login", message);
    }

    // 입력 오류시 호출
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
            return buildView("login/forgot-password", message);
        }
        if (path.endsWith("/reset-password")) {
            return buildView("login/reset-password", e.getMessage());
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
