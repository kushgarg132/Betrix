package com.example.backend.resolver;

import com.example.backend.entity.Game;
import com.example.backend.entity.User;
import com.example.backend.model.ActionPayload;
import com.example.backend.model.BlindPayload;
import com.example.backend.model.ChatMessagePayload;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.BotDifficulty;
import com.example.backend.model.LoginInput;
import com.example.backend.model.Player;
import com.example.backend.model.RegisterInput;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.BotService;
import com.example.backend.service.GameNotificationService;
import com.example.backend.service.GameService;
import com.example.backend.service.UserService;
import graphql.GraphqlErrorException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
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

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final GameService gameService;
    private final GameNotificationService notificationService;
    private final BotService botService;

    @MutationMapping
    public Map<String, Object> login(@Argument @Valid LoginInput input) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(input.username(), input.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);
            return Map.of("token", jwt, "type", "Bearer");
        } catch (BadCredentialsException e) {
            throw GraphqlErrorException.newErrorException()
                    .message("Invalid username or password.")
                    .errorClassification(ErrorType.UNAUTHORIZED)
                    .build();
        }
    }

    @MutationMapping
    public User register(@Argument @Valid RegisterInput input) {
        return userService.createUser(input.name(), input.username(), input.password(), input.email());
    }

    @MutationMapping
    public Map<String, Object> guestLogin() {
        String username = "guest-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String token = jwtTokenProvider.generateGuestToken(username);
        return Map.of("token", token, "type", "Bearer");
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
        String playerId = (String) input.get("playerId");
        String actionType = (String) input.get("actionType");
        Number amount = (Number) input.get("amount");

        switch (ActionPayload.ActionType.valueOf(actionType)) {
            case CHECK -> gameService.check(gameId, playerId);
            case BET -> gameService.placeBet(gameId, playerId, amount != null ? amount.doubleValue() : 0);
            case FOLD -> gameService.fold(gameId, playerId);
            case LEAVE -> gameService.leaveGame(gameId, playerId);
            case SIT_OUT -> gameService.sitOut(gameId, playerId);
            case SIT_IN -> gameService.sitIn(gameId, playerId);
        }
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean startHand(@Argument String gameId) {
        gameService.startNewHand(gameId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Player addBot(@Argument String gameId, @Argument BotDifficulty difficulty) {
        return botService.addBot(gameId, difficulty);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean removeBot(@Argument String gameId, @Argument String botPlayerId) {
        botService.removeBot(gameId, botPlayerId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ChatMessagePayload sendChat(
            @Argument String gameId,
            @Argument String message,
            @Argument String playerId) {
        ChatMessagePayload chat = new ChatMessagePayload();
        chat.setSenderId(playerId);
        chat.setMessage(message);
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
