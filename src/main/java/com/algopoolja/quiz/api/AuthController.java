package com.algopoolja.quiz.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Number userId)) {
            return Map.of("authenticated", false);
        }
        return Map.of(
                "authenticated", true,
                "userId", userId.longValue(),
                "nickname", value(session, "nickname"),
                "email", value(session, "email"),
                "picture", value(session, "picture")
        );
    }

    private String value(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value == null ? "" : value.toString();
    }
}
