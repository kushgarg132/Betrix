package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed-admin", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private static final int MIN_ADMIN_PASSWORD = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.email:admin@betrix.com}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        ensureUserIndexes();
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            if (adminPassword == null || adminPassword.length() < MIN_ADMIN_PASSWORD) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD must be set to at least " + MIN_ADMIN_PASSWORD + " characters to create the admin account");
            }
            User admin = new User();
            admin.setName("Admin");
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEmail(adminEmail);
            admin.setBalance(0);
            admin.setRoles(List.of("ADMIN"));
            userRepository.save(admin);
            logger.info("Admin account created: username={}", adminUsername);
        }
    }

    // Backs up the check-then-insert in UserService.createUser. Mongo does not create indexes for
    // annotations by default, and existing duplicate data would make creation fail, so a failure is
    // logged rather than allowed to stop the app from starting.
    private void ensureUserIndexes() {
        try {
            var indexes = mongoTemplate.indexOps(User.class);
            indexes.ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
            indexes.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique().sparse());
        } catch (Exception e) {
            logger.error("Could not create unique indexes on users (duplicate usernames or emails?): {}", e.getMessage());
        }
    }
}
