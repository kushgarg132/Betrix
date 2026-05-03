package com.example.backend.config;

import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.UserService;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class GraphQLAuthInterceptor implements WebGraphQlInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public GraphQLAuthInterceptor(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);

                UsernamePasswordAuthenticationToken auth;
                if (username != null && username.startsWith("guest-")) {
                    // Guest users get a GUEST authority
                    auth = new UsernamePasswordAuthenticationToken(
                            username, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST")));
                } else {
                    // Load actual user from DB to get their real roles/authorities
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                }
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        return chain.next(request);
    }
}
