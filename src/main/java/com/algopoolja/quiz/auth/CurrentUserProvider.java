package com.algopoolja.quiz.auth;

import com.algopoolja.quiz.learning.UserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {
    private final JwtTokenVerifier jwtTokenVerifier;

    public UserIdentity requireCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") instanceof Number userId) {
            Object nickname = session.getAttribute("nickname");
            return new UserIdentity(userId.longValue(), nickname == null ? "사용자" : nickname.toString());
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return jwtTokenVerifier.verify(authorization.substring(7).trim());
        }
        throw new UnauthorizedException("로그인이 필요합니다.");
    }
}
