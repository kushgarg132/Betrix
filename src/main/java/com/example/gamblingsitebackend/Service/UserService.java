package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.User;
import com.example.gamblingsitebackend.Repository.UserRepository;
import com.example.gamblingsitebackend.Utility.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserService(WalletService walletService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.walletService = walletService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username).stream().findFirst().orElse(null);
        if (user != null && user.getPassword().equals(password)) {
            return jwtUtil.generateToken(username);
        }
        throw new RuntimeException("Invalid username or password.");
    }

    public User registerUser(User user , double initialBalance) {
        if (userRepository.findByUsername(user.getUsername()).stream().findFirst().isPresent()) {
            throw new RuntimeException("Username already exists.");
        }
        walletService.createWallet(user.getUsername(), initialBalance);
        // TODO: Add password encryption (e.g., BCrypt)
        return userRepository.save(user);
    }
}
