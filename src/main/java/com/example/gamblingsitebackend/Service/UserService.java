package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.User;
import com.example.gamblingsitebackend.Repository.UserRepository;
import com.example.gamblingsitebackend.Utility.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
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

    public User registerUser(User user) {
        // TODO: Add password encryption (e.g., BCrypt)
        return userRepository.save(user);
    }
}
