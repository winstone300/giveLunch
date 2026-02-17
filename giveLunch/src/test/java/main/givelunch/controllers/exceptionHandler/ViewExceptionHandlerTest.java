package main.givelunch.controllers.exceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

class ViewExceptionHandlerTest {

    private final ViewExceptionHandler handler = new ViewExceptionHandler();

    @Test
    @DisplayName("ValidationException /forgot-password: 사용자 식별 오류는 공통 안내 문구를 반환")
    void handleValidationExceptionOnForgotPasswordReturnsGenericMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/forgot-password/");

        ModelAndView modelAndView = handler.handleValidationException(
                new ValidationException(ErrorCode.USER_NOT_FOUND),
                request
        );

        assertThat(modelAndView.getViewName()).isEqualTo("login/forgot-password");
        assertThat(modelAndView.getModel().get("error")).isEqualTo("아이디 또는 이메일이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("BindException /forgot-password: 필드 메시지 대신 공통 안내 문구를 반환")
    void handleBindExceptionOnForgotPasswordReturnsGenericMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/forgot-password");

        BindException bindException = new BindException(new Object(), "request");
        bindException.addError(new FieldError("request", "email", "잘못된 이메일"));

        ModelAndView modelAndView = handler.handleBindException(bindException, request);

        assertThat(modelAndView.getViewName()).isEqualTo("login/forgot-password");
        assertThat(modelAndView.getModel().get("error")).isEqualTo("아이디 또는 이메일이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("BindException 루트 경로: 로그인 화면으로 fallback")
    void handleBindExceptionOnRootPathFallsBackToLogin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/");

        BindException bindException = new BindException(new Object(), "request");
        bindException.addError(new FieldError("request", "userName", "아이디를 입력해주세요."));

        ModelAndView modelAndView = handler.handleBindException(bindException, request);

        assertThat(modelAndView.getViewName()).isEqualTo("login/login");
        assertThat(modelAndView.getModel().get("error")).isEqualTo("아이디를 입력해주세요.");
    }
}
