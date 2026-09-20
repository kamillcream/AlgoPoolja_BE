package com.algopoolja.quiz.auth;

import com.algopoolja.quiz.learning.UserIdentity;
import com.algopoolja.quiz.persistence.entity.UserEntity;
import com.algopoolja.quiz.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthUserService {
    private final UserRepository userRepository;

    @Transactional
    public UserIdentity loginOrSignUp(String email, String nickname) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Google 계정의 이메일을 확인할 수 없습니다.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        String safeNickname = nickname == null || nickname.isBlank() ? "사용자" : nickname.trim();
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> {
                    existing.updateNickname(safeNickname);
                    return existing;
                })
                .orElseGet(() -> userRepository.save(new UserEntity(normalizedEmail, safeNickname)));
        return new UserIdentity(user.getId(), user.getNickname());
    }
}
