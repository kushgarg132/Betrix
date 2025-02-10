package com.example.gamblingsitebackend.Controller;

import com.example.gamblingsitebackend.Entity.User;
import com.example.gamblingsitebackend.Service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody User user, @RequestParam double initialBalance) {
        return userService.registerUser(user, initialBalance);
    }

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        return userService.login(user.getUsername(), user.getPassword());
    }
}
