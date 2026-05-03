package com.example.backend.resolver;

import com.example.backend.entity.Game;
import com.example.backend.entity.GameEvent;
import com.example.backend.entity.User;
import com.example.backend.repository.GameEventRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.GameReplayService;
import com.example.backend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class QueryResolver {

    private final GameService gameService;
    private final UserRepository userRepository;
    private final GameEventRepository gameEventRepository;
    private final GameReplayService gameReplayService;

    @QueryMapping
    public User me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        String username = auth.getName();
        
        // Check if it's a guest user
        if (username != null && username.startsWith("guest-")) {
            User guest = new User();
            guest.setId(username); // Use username as ID for guests
            guest.setUsername(username);
            guest.setName("Guest");
            guest.setBalance(1000); // Same starting balance as registered users
            guest.setRoles(java.util.List.of("GUEST")); // Ensure roles is not null
            return guest;
        }
        
        return userRepository.findByUsername(username).orElse(null);
    }

    @QueryMapping
    public List<Game> games() {
        return gameService.getAllGames();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Game game(@Argument String id) {
        return gameService.getGameForPlayer(id, null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Game gameForPlayer(@Argument String gameId, @Argument String playerId) {
        return gameService.getGameForPlayer(gameId, playerId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<GameEvent> gameEvents(@Argument String gameId) {
        return gameEventRepository.findByGameIdOrderByTimestampAsc(gameId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<GameEvent> gameEventsByType(@Argument String gameId, @Argument String eventType) {
        return gameEventRepository.findByGameIdAndEventTypeOrderByTimestampAsc(gameId, eventType);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Game replayGame(@Argument String gameId) {
        return gameReplayService.replayGame(gameId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Game replayGameUntilEvent(@Argument String gameId, @Argument String eventId) {
        return gameReplayService.replayGameUntilEvent(gameId, eventId);
    }
}
