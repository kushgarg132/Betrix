package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BotActionService {
    private static final Logger logger = LoggerFactory.getLogger(BotActionService.class);

    private static final String FLASH_LITE_MODEL = "gemini-1.5-flash";
    private static final String PRO_MODEL = "gemini-1.5-pro";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final GameService gameService;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public void takeTurn(String gameId, String botPlayerId) {
        try {
            // Re-fetch latest game state before acting
            Game game = gameRepository.findById(gameId).orElse(null);
            if (game == null || game.getStatus() == Game.GameStatus.WAITING
                    || game.getStatus() == Game.GameStatus.FINISHED) {
                return;
            }

            Player bot = game.getPlayers().stream()
                    .filter(p -> p.getId().equals(botPlayerId))
                    .findFirst().orElse(null);

            if (bot == null || !game.isPlayersTurn(botPlayerId)) {
                return;
            }

            String difficulty = bot.getBotDifficulty() != null ? bot.getBotDifficulty() : "MEDIUM";
            String model = "HARD".equals(difficulty) ? PRO_MODEL : FLASH_LITE_MODEL;

            GeminiAction action = callGemini(model, buildSystemPrompt(difficulty), buildGameStatePrompt(game, bot));
            executeAction(gameId, game, bot, action);
        } catch (Exception e) {
            logger.error("Bot turn error for player {} in game {}: {}", botPlayerId, gameId, e.getMessage());
            try {
                gameService.fold(gameId, botPlayerId);
            } catch (Exception ignored) {}
        }
    }

    private String buildSystemPrompt(String difficulty) {
        String persona = switch (difficulty) {
            case "EASY"   -> "You play conservatively. Fold weak hands (nothing paired), call with pairs, raise with two pair or better.";
            case "HARD"   -> "You play GTO-adjacent. Consider hand ranges, position, stack-to-pot ratio, and implied odds.";
            default       -> "You play solid fundamentals. Use pot odds to decide calls. Raise strong hands, fold weak ones.";
        };
        return """
                You are a Texas Hold'em poker bot (%s difficulty).
                %s
                Rules: FOLD = give up hand, CHECK = pass if no bet, CALL = match current bet, RAISE = increase bet.
                You MUST respond with ONLY valid JSON, no explanation: {"action":"FOLD"|"CHECK"|"CALL"|"RAISE","amount":0}
                For RAISE, amount is the total chips to put in (not the raise-by amount). For other actions, amount is 0.
                """.formatted(difficulty.toLowerCase(), persona);
    }

    private String buildGameStatePrompt(Game game, Player bot) {
        long activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isHasFolded() && p.isActive()).count();

        StringBuilder sb = new StringBuilder();
        sb.append("=== GAME STATE ===\n");
        sb.append("Round: ").append(game.getStatus()).append("\n");
        sb.append("Pot: ").append(game.getPot()).append("\n");
        sb.append("Current bet to call: ").append(callAmount(game, bot)).append("\n");
        sb.append("Small blind: ").append(game.getSmallBlindAmount())
          .append(" | Big blind: ").append(game.getBigBlindAmount()).append("\n");
        sb.append("Players still in hand: ").append(activePlayers).append("\n\n");

        sb.append("=== YOUR HAND ===\n");
        if (bot.getHand() != null) {
            bot.getHand().forEach(c -> sb.append(c.getRank()).append(" of ").append(c.getSuit()).append("\n"));
        }
        sb.append("Your chips: ").append(bot.getChips()).append("\n\n");

        sb.append("=== COMMUNITY CARDS ===\n");
        if (game.getCommunityCards() != null && !game.getCommunityCards().isEmpty()) {
            game.getCommunityCards().forEach(c -> sb.append(c.getRank()).append(" of ").append(c.getSuit()).append("\n"));
        } else {
            sb.append("(none yet)\n");
        }

        sb.append("\n=== OTHER PLAYERS ===\n");
        game.getPlayers().stream()
                .filter(p -> !p.getId().equals(bot.getId()))
                .forEach(p -> sb.append(p.getUsername())
                        .append(": chips=").append(p.getChips())
                        .append(", folded=").append(p.isHasFolded())
                        .append(", bet=").append(p.getCurrentBet()).append("\n"));

        return sb.toString();
    }

    private GeminiAction callGemini(String model, String system, String user) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Gemini API key not set — bot using random fallback");
            return randomFallbackAction();
        }
        try {
            String url = GEMINI_URL.formatted(model, apiKey);
            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", system))),
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", user)))),
                    "generationConfig", Map.of("temperature", 0.4, "responseMimeType", "application/json")
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String response = restTemplate.postForObject(
                    url, new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
            JsonNode root = objectMapper.readTree(response);
            String text = root.at("/candidates/0/content/parts/0/text").asText();
            return objectMapper.readValue(text, GeminiAction.class);
        } catch (Exception e) {
            logger.warn("Gemini call failed: {} — using random fallback", e.getMessage());
            return randomFallbackAction();
        }
    }

    private void executeAction(String gameId, Game game, Player bot, GeminiAction action) {
        try {
            switch (action.action()) {
                case "CHECK" -> {
                    double toCall = callAmount(game, bot);
                    if (toCall <= 0) {
                        gameService.check(gameId, bot.getId());
                    } else {
                        // Can't check when there's a bet — call instead
                        gameService.placeBet(gameId, bot.getId(), toCall);
                    }
                }
                case "CALL"  -> gameService.placeBet(gameId, bot.getId(), callAmount(game, bot));
                case "RAISE" -> {
                    double raiseAmount = Math.min(action.amount(), bot.getChips());
                    if (raiseAmount > game.getCurrentBet()) {
                        gameService.placeBet(gameId, bot.getId(), raiseAmount);
                    } else {
                        gameService.placeBet(gameId, bot.getId(), callAmount(game, bot));
                    }
                }
                default      -> gameService.fold(gameId, bot.getId());
            }
        } catch (Exception e) {
            logger.error("Bot action execution failed: {}", e.getMessage());
            try { gameService.fold(gameId, bot.getId()); } catch (Exception ignored) {}
        }
    }

    private double callAmount(Game game, Player bot) {
        double alreadyBet = game.getCurrentBettingRound() != null
                ? game.getCurrentBettingRound().getBets().getOrDefault(bot.getId(), 0.0)
                : 0.0;
        return Math.min(game.getCurrentBet() - alreadyBet, bot.getChips());
    }

    private GeminiAction randomFallbackAction() {
        double r = Math.random();
        // Bias toward CALL/CHECK; only fold ~15% of the time
        if (r < 0.15) return new GeminiAction("FOLD", 0);
        if (r < 0.75) return new GeminiAction("CALL", 0);
        return new GeminiAction("CHECK", 0);
    }

    record GeminiAction(String action, int amount) {}
}
