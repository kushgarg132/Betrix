package com.example.backend.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterInput(
    @NotBlank @Size(min = 2, max = 50) String name,
    // No hyphen on purpose: "guest-" and "bot-" prefixes identify system-created players
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$",
            message = "may only contain letters, numbers and underscores (3-30 characters)") String username,
    // bcrypt ignores everything past 72 bytes
    @NotBlank @Size(min = 6, max = 72) String password,
    @Email @NotBlank @Size(max = 254) String email
) {}
