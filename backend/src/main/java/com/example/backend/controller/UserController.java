package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();

        if (username != null && username.startsWith("guest-")) {
            User guest = new User();
            guest.setUsername(username);
            guest.setName("Guest");
            guest.setRoles(List.of("GUEST"));
            guest.setBalance(10000);
            return ResponseEntity.ok(guest);
        }

        User user = (User) userService.loadUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/add-balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateBalance(Authentication authentication, @RequestParam("amount") int amount) {
        String username = authentication.getName();

        if (username != null && username.startsWith("guest-")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User users = userService.addBalance(username, amount);
        return ResponseEntity.ok(users);
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
    }
}