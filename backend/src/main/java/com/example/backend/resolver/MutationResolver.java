package com.example.backend.resolver;

import com.example.backend.entity.Game;
import com.example.backend.entity.User;
import com.example.backend.model.ActionType;
import com.example.backend.model.BlindPayload;
import com.example.backend.model.ChatMessagePayload;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.BotDifficulty;
import com.example.backend.model.Player;
import com.example.backend.security.AuthRateLimiter;
import com.example.backend.security.ClientIp;
import com.example.backend.security.CurrentUser;
import com.example.backend.security.GoogleIdTokenVerifier;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.BotService;
import com.example.backend.service.GameNotificationService;
import com.example.backend.service.GameValidatorService;
import com.example.backend.service.GameService;
import com.example.backend.service.UserService;
import graphql.GraphqlErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class MutationResolver {

    private static final int MAX_CHAT_LENGTH = 500;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final GameService gameService;
    private final GameNotificationService notificationService;
    private final BotService botService;
    private final GameValidatorService gameValidatorService;
    private final AuthRateLimiter authRateLimiter;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @MutationMapping
    public Map<String, Object> guestLogin() {
        authRateLimiter.check(ClientIp.current());
        String username = com.example.backend.security.PlayerIdentity.newGuestUsername();
        String token = jwtTokenProvider.generateGuestToken(username);
        return Map.of("token", token, "type", "Bearer");
    }

    @MutationMapping
    public Map<String, Object> googleLogin(@Argument String idToken) {
        authRateLimiter.check(ClientIp.current());
        GoogleIdTokenVerifier.GoogleIdentity identity;
        try {
            identity = googleIdTokenVerifier.verify(idToken);
        } catch (BadCredentialsException e) {
            throw GraphqlErrorException.newErrorException()
                    .message("Google sign-in failed. Try again.")
                    .errorClassification(ErrorType.UNAUTHORIZED)
                    .build();
        }
        User user;
        try {
            user = userService.signInWithGoogle(identity);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw GraphqlErrorException.newErrorException()
                    .message("Could not sign in with this Google account. Try again.")
                    .errorClassification(ErrorType.UNAUTHORIZED)
                    .build();
        }
        String jwt = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return Map.of("token", jwt, "type", "Bearer");
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Game createGame(@Argument Map<String, Integer> input) {
        BlindPayload payload = new BlindPayload();
        payload.setSmallBlindAmount(input.get("smallBlindAmount"));
        payload.setBigBlindAmount(input.get("bigBlindAmount"));
        gameService.createGame(payload);
        var games = gameService.getAllGames();
        return games.getLast();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Game joinGame(@Argument String gameId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return gameService.joinGame(gameId, auth.getName());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteGame(@Argument String gameId) {
        return gameService.deleteGame(gameId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public User addBalance(@Argument int amount) {
        throw GraphqlErrorException.newErrorException()
                .message("Payment processing is not yet available.")
                .errorClassification(ErrorType.FORBIDDEN)
                .build();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean playerAction(@Argument String gameId, @Argument Map<String, Object> input) {
        Game game = gameValidatorService.validateGameExists(gameId);
        String playerId = gameValidatorService.requireOwnPlayer(game, CurrentUser.username()).getId();
        String actionType = (String) input.get("actionType");
        Number amount = (Number) input.get("amount");

        switch (ActionType.valueOf(actionType)) {
            case CHECK -> gameService.check(gameId, playerId);
            case BET -> gameService.placeBet(gameId, playerId, amount != null ? amount.longValue() : 0);
            case FOLD -> gameService.fold(gameId, playerId);
        }
        return true;
    }

    /** Leaving, sitting out and sitting in are not turn actions -- they are not routed through
     * playerAction, and can happen any time, not just on the caller's turn. */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean leaveGame(@Argument String gameId) {
        Game game = gameValidatorService.validateGameExists(gameId);
        String playerId = gameValidatorService.requireOwnPlayer(game, CurrentUser.username()).getId();
        gameService.leaveGame(gameId, playerId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean sitOut(@Argument String gameId) {
        Game game = gameValidatorService.validateGameExists(gameId);
        String playerId = gameValidatorService.requireOwnPlayer(game, CurrentUser.username()).getId();
        gameService.sitOut(gameId, playerId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean sitIn(@Argument String gameId) {
        Game game = gameValidatorService.validateGameExists(gameId);
        String playerId = gameValidatorService.requireOwnPlayer(game, CurrentUser.username()).getId();
        gameService.sitIn(gameId, playerId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean startHand(@Argument String gameId) {
        gameValidatorService.validateSeated(gameValidatorService.validateGameExists(gameId), CurrentUser.username());
        gameService.startNewHand(gameId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Player addBot(@Argument String gameId, @Argument BotDifficulty difficulty) {
        gameValidatorService.validateSeated(gameValidatorService.validateGameExists(gameId), CurrentUser.username());
        return botService.addBot(gameId, difficulty);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean removeBot(@Argument String gameId, @Argument String botPlayerId) {
        Game game = gameValidatorService.validateGameExists(gameId);
        gameValidatorService.validateSeated(game, CurrentUser.username());
        Player target = game.getPlayerById(botPlayerId);
        if (target == null || !target.isBot()) {
            throw new IllegalArgumentException("Not a bot at this table");
        }
        botService.removeBot(gameId, botPlayerId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ChatMessagePayload sendChat(@Argument String gameId, @Argument String message) {
        Player sender = gameValidatorService.requireOwnPlayer(
                gameValidatorService.validateGameExists(gameId), CurrentUser.username());
        if (message == null || message.isBlank() || message.length() > MAX_CHAT_LENGTH) {
            throw new IllegalArgumentException("Message must be 1-" + MAX_CHAT_LENGTH + " characters");
        }
        ChatMessagePayload chat = new ChatMessagePayload();
        chat.setSenderId(sender.getId());
        chat.setSenderName(sender.getName());
        chat.setMessage(message.strip());
        chat.setTimestamp(OffsetDateTime.now());

        notificationService.notifyGameUpdate(
                GameUpdate.builder()
                        .gameId(gameId)
                        .type(GameUpdate.GameUpdateType.CHAT_MESSAGE)
                        .payload(chat)
                        .timestamp(OffsetDateTime.now())
                        .build());
        return chat;
    }
}
