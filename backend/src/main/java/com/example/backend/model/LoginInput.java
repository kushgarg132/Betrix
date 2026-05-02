package com.example.backend.model;

import jakarta.validation.constraints.NotBlank;

public record LoginInput(
    @NotBlank String username,
    @NotBlank String password
) {}
