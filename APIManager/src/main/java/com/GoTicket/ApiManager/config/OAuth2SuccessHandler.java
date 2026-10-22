package com.GoTicket.ApiManager.config;

import com.GoTicket.ApiManager.service.JwtService;
import com.GoTicket.ApiManager.service.ProxyAuthorizationService;
import com.GoTicket.ApiManager.service.UsuarioClient;
import com.GoTicket.ApiManager.model.UsuarioPerfilResponse;
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
    private final UsuarioClient usuarioClient;

    @Value("${app.oauth2.frontend-success-url:http://localhost:5173/#oauth2}")
    private String frontendSuccessUrl;

    public OAuth2SuccessHandler(JwtService jwtService, UsuarioClient usuarioClient) {
        this.jwtService = jwtService;
        this.usuarioClient = usuarioClient;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");
        String googleSubject = user.getAttribute("sub");
        if (email == null || email.isBlank() || googleSubject == null || googleSubject.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google no devolvió una identidad verificable.");
            return;
        }
        String role = isAdminEmail(email) ? "ADMIN" : "CLIENTE";
        Long legacyUserId = Long.valueOf(ProxyAuthorizationService.deriveUserId(email));
        UsuarioPerfilResponse profile = usuarioClient.sincronizarIdentidadGoogle(
                legacyUserId, googleSubject, email, name == null ? email : name);
        String token = jwtService.generateToken(profile.idUsuario(), profile.email(), profile.nombre(), role);
        String userId = String.valueOf(profile.idUsuario());
        String targetUrl = frontendSuccessUrl
            + "?token=" + encode(token)
            + "&idUsuario=" + encode(userId)
            + "&email=" + encode(profile.email())
            + "&name=" + encode(profile.nombre());
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isAdminEmail(String email) {
        return email != null && email.trim().toLowerCase().endsWith("@duocuc.cl");
    }
}