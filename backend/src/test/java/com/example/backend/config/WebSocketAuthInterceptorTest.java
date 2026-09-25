package com.example.backend.config;

import com.example.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private JwtTokenProvider jwt;
    private WebSocketAuthInterceptor interceptor;
    private WebSocketSessionInfo session;
    private final Map<String, Object> attributes = new HashMap<>();

    @BeforeEach
    void setUp() {
        jwt = mock(JwtTokenProvider.class);
        when(jwt.validateToken("good")).thenReturn(true);
        when(jwt.getUsernameFromToken("good")).thenReturn("alice");
        interceptor = new WebSocketAuthInterceptor(jwt);
        session = mock(WebSocketSessionInfo.class);
        when(session.getAttributes()).thenReturn(attributes);
    }

    private Object contextUsernameSeenByOperation() {
        WebSocketGraphQlRequest request = new WebSocketGraphQlRequest(
                URI.create("ws://localhost/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                Map.of(), Map.of("query", "subscription { __typename }"), "1", Locale.ENGLISH, session);
        AtomicReference<WebGraphQlRequest> forwarded = new AtomicReference<>();
        interceptor.intercept(request, r -> {
            forwarded.set(r);
            return Mono.empty();
        }).block();
        return forwarded.get().toExecutionInput().getGraphQLContext().get(WebSocketAuthInterceptor.USERNAME);
    }

    @Test
    void tokenInConnectionInitPayloadIdentifiesEveryOperationOnTheConnection() {
        interceptor.handleConnectionInitialization(session, Map.of("Authorization", "Bearer good")).block();

        assertEquals("alice", contextUsernameSeenByOperation());
    }

    @Test
    void missingOrInvalidTokenLeavesTheConnectionAnonymous() {
        interceptor.handleConnectionInitialization(session, Map.of("Authorization", "Bearer forged")).block();
        assertNull(contextUsernameSeenByOperation());

        interceptor.handleConnectionInitialization(session, Map.of("Authorization", "")).block();
        interceptor.handleConnectionInitialization(session, Map.of()).block();
        assertNull(contextUsernameSeenByOperation());
    }
}
