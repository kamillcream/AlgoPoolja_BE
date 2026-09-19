package com.algopoolja.quiz.auth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class OAuthUserIdentity {
    private OAuthUserIdentity() {
    }

    static long stableUserId(String provider, String subject) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((provider + ":" + subject).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
