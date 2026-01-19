package com.codegym.appticket.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        System.out.println("DEBUG: [LOGIN_FAILED] Error: " + exception.getMessage());
        if (exception.getCause() != null) {
            System.out.println("DEBUG: [LOGIN_FAILED] Cause: " + exception.getCause().getMessage());
        }
        response.sendRedirect("/login?error=true");
    }
}
