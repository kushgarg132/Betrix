package com.example.gamblingsitebackend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

    // Inject permitted endpoints from application.properties or application.yml
    @Value("${security.config.permitted-endpoints}")
    private List<String> permittedEndpoints;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permittedEndpoints.toArray(new String[0])).permitAll() // Externalized endpoints
                        .anyRequest().authenticated() // Protect all other endpoints
                );
        return http.build();
    }
}