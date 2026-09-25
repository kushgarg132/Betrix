package com.example.backend.resolver;

import com.example.backend.entity.User;
import com.example.backend.repository.GameRepository;
import com.example.backend.security.AuthRateLimiter;
import com.example.backend.security.GoogleIdTokenVerifier;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.BotService;
import com.example.backend.service.GameNotificationService;
import com.example.backend.service.GameService;
import com.example.backend.service.GameValidatorService;
import com.example.backend.service.UserService;
import graphql.GraphqlErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * C1: a DuplicateKeyException from signInWithGoogle that ISN'T the googleSub race (e.g. the legacy
 * unique index on email colliding with an old password-era account) must surface as a clear,
 * classified GraphQL error - never an opaque INTERNAL_ERROR.
 */
class GoogleLoginDuplicateKeyTest {

    private MutationResolver mutations;
    private UserService userService;
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        verifier = mock(GoogleIdTokenVerifier.class);
        GameValidatorService validator = new GameValidatorService(mock(GameRepository.class),
                mock(com.example.backend.repository.UserRepository.class));
        mutations = new MutationResolver(mock(JwtTokenProvider.class), userService, mock(GameService.class),
                mock(GameNotificationService.class), mock(BotService.class), validator, new AuthRateLimiter(1000),
                verifier);
    }

    @Test
    void aNonRaceDuplicateKeyCollisionSurfacesAsAClearError() {
        when(verifier.verify("token")).thenReturn(new GoogleIdTokenVerifier.GoogleIdentity(
                "sub-1", "dup@example.com", "Someone", null));
        when(userService.signInWithGoogle(any())).thenThrow(new DuplicateKeyException("email index"));

        GraphqlErrorException thrown = assertThrows(GraphqlErrorException.class,
                () -> mutations.googleLogin("token"));

        assertEquals("UNAUTHORIZED", thrown.getErrorType().toString());
        assertEquals("Could not sign in with this Google account. Try again.", thrown.getMessage());
    }
}
