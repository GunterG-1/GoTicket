package com.GoTicket.ApiManager.config;

import com.GoTicket.ApiManager.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.oauth2.frontend-success-url:http://localhost:5173/#oauth2}")
    private String frontendSuccessUrl;

    public OAuth2SuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");
        String role = isAdminEmail(email) ? "ADMIN" : "CLIENTE";
        String token = jwtService.generateToken(email, name, role);
        String userId = deriveUserId(email);
        String targetUrl = frontendSuccessUrl
            + "?token=" + encode(token)
            + "&idUsuario=" + encode(userId)
            + "&email=" + encode(email)
            + "&name=" + encode(name);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String deriveUserId(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        return String.valueOf(Math.abs(email.trim().toLowerCase().hashCode()));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isAdminEmail(String email) {
        return email != null && email.trim().toLowerCase().endsWith("@duocuc.cl");
    }
}