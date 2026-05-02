package com.example.backend.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterInput(
    @NotBlank @Size(min = 2, max = 50) String name,
    @NotBlank @Size(min = 3, max = 30) String username,
    @NotBlank @Size(min = 6) String password,
    @Email @NotBlank String email
) {}
