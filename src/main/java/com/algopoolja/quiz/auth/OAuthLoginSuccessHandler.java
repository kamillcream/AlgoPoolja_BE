package com.algopoolja.quiz.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final String frontendUrl;

    public OAuthLoginSuccessHandler(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String subject = oauthUser.getAttribute("sub");
        String nickname = oauthUser.getAttribute("name");
        String email = oauthUser.getAttribute("email");

        if (subject == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google 사용자 정보를 확인할 수 없습니다.");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", OAuthUserIdentity.stableUserId("google", subject));
        session.setAttribute("nickname", nickname == null || nickname.isBlank() ? "사용자" : nickname);
        session.setAttribute("email", email);
        session.setAttribute("picture", oauthUser.getAttribute("picture"));

        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("view", "dashboard")
                .build().toUriString();
        response.sendRedirect(target);
    }
}
