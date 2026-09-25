package com.example.backend.config;

import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * The browser sends the JWT in the graphql-ws connection_init payload, not in an HTTP header, so
 * GraphQLAuthInterceptor never sees it. Read it once per connection and expose the username to
 * every operation on that connection as the GraphQL context value {@link #USERNAME}.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements WebSocketGraphQlInterceptor {

    public static final String USERNAME = "username";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Object> handleConnectionInitialization(WebSocketSessionInfo info, Map<String, Object> payload) {
        Object header = payload.get("Authorization");
        if (header instanceof String h && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                info.getAttributes().put(USERNAME, jwtTokenProvider.getUsernameFromToken(token));
            }
        }
        return Mono.empty();
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (request instanceof WebSocketGraphQlRequest ws) {
            Object username = ws.getSessionInfo().getAttributes().get(USERNAME);
            if (username != null) {
                request.configureExecutionInput((in, b) -> b.graphQLContext(c -> c.put(USERNAME, username)).build());
            }
        }
        return chain.next(request);
    }
}
