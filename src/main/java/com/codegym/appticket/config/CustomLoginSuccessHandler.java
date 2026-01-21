package com.codegym.appticket.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final org.springframework.security.web.savedrequest.RequestCache requestCache = new org.springframework.security.web.savedrequest.HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. Check for Saved Request (e.g. user accessed /events/create but was forced to login)
        org.springframework.security.web.savedrequest.SavedRequest savedRequest = requestCache.getRequest(request, response);
        
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            // Optional: Prevent redirecting to login/register pages if they somehow got saved
            if (!targetUrl.contains("/login") && !targetUrl.contains("/register")) {
                response.sendRedirect(targetUrl);
                return;
            }
        }

        // 2. Default Role-based Validation
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else if (roles.contains("STAFF")) {
            response.sendRedirect("/admin/events");
        } else {
            response.sendRedirect("/");
        }
    }
}
