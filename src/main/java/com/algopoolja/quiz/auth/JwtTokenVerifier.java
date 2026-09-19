package com.algopoolja.quiz.auth;

import com.algopoolja.quiz.learning.UserIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JwtTokenVerifier {
    private final byte[] secret;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public JwtTokenVerifier(@Value("${app.auth.jwt-secret:}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public UserIdentity verify(String token) {
        if (secret.length < 32) {
            throw new UnauthorizedException("Bearer 인증이 설정되지 않았습니다.");
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new UnauthorizedException("유효하지 않은 토큰입니다.");
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) throw new UnauthorizedException("유효하지 않은 토큰입니다.");

            JsonNode header = jsonMapper.readTree(decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) throw new UnauthorizedException("지원하지 않는 토큰입니다.");
            JsonNode payload = jsonMapper.readTree(decode(parts[1]));
            long expiresAt = payload.path("exp").asLong(0);
            if (expiresAt > 0 && Instant.now().getEpochSecond() >= expiresAt) {
                throw new UnauthorizedException("인증 정보가 만료되었습니다.");
            }
            long userId = payload.path("userId").asLong(payload.path("user_id").asLong(0));
            if (userId <= 0) throw new UnauthorizedException("토큰에 사용자 정보가 없습니다.");
            String nickname = payload.path("nickname").asText(payload.path("sub").asText("사용자"));
            return new UserIdentity(userId, nickname);
        }
        catch (UnauthorizedException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }
    }

    private byte[] hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
