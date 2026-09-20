package com.algopoolja.quiz.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.algopoolja.quiz.persistence.entity.UserEntity;
import com.algopoolja.quiz.persistence.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OAuthUserServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final OAuthUserService service = new OAuthUserService(repository);

    @Test
    void signsUpWhenGoogleEmailDoesNotExist() {
        UserEntity saved = mock(UserEntity.class);
        when(saved.getId()).thenReturn(12L);
        when(saved.getNickname()).thenReturn("알고풀자");
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(UserEntity.class))).thenReturn(saved);

        var identity = service.loginOrSignUp("USER@example.com", "알고풀자");

        assertThat(identity.getUserId()).isEqualTo(12L);
        assertThat(identity.getNickname()).isEqualTo("알고풀자");
        verify(repository).save(any(UserEntity.class));
    }

    @Test
    void reusesExistingUserAndUpdatesNickname() {
        UserEntity existing = mock(UserEntity.class);
        when(existing.getId()).thenReturn(12L);
        when(existing.getNickname()).thenReturn("새 닉네임");
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existing));

        var identity = service.loginOrSignUp("user@example.com", "새 닉네임");

        assertThat(identity.getUserId()).isEqualTo(12L);
        verify(existing).updateNickname("새 닉네임");
        verify(repository, never()).save(any());
    }
}
