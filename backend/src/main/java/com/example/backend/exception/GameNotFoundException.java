package com.example.backend.exception;

/** The game, or a player within it, does not exist. Maps to NOT_FOUND. */
public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String message) {
        super(message);
    }
}
