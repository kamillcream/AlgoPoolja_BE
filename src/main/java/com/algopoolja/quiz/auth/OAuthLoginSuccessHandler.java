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
import com.algopoolja.quiz.learning.UserIdentity;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final String frontendUrl;
    private final OAuthUserService oauthUserService;

    public OAuthLoginSuccessHandler(@Value("${app.frontend-url}") String frontendUrl,
                                    OAuthUserService oauthUserService) {
        this.frontendUrl = frontendUrl;
        this.oauthUserService = oauthUserService;
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

        UserIdentity user = oauthUserService.loginOrSignUp(email, nickname);
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("nickname", user.getNickname());
        session.setAttribute("email", email);
        session.setAttribute("picture", oauthUser.getAttribute("picture"));
        log.info("OAuth login completed: userId={}", user.getUserId());

        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("view", "dashboard")
                .build().toUriString();
        response.sendRedirect(target);
    }
}
