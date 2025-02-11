package com.example.gamblingsitebackend.Auth;
import com.example.gamblingsitebackend.Repository.UserRepository;
import com.example.gamblingsitebackend.Service.WalletService;
import com.example.gamblingsitebackend.Utility.JwtService;
import org.springframework.web.bind.annotation.*;
import com.example.gamblingsitebackend.Entity.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final WalletService walletService;

    public AuthController(UserRepository userRepository, JwtService jwtService, WalletService walletService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.walletService = walletService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        user.setPassword(user.getPassword());
        walletService.createWallet(user.getUsername(), 5000);
        userRepository.save(user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return response;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User user) {
        User existingUser = userRepository.findByUsername(user.getUsername()).stream().findFirst().orElse(null);
        if (existingUser != null && user.getPassword().matches(existingUser.getPassword())) {
            String token = jwtService.generateToken(user.getUsername());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return response;
        }
        else {
            throw new RuntimeException("Invalid username or password");
        }
    }
}