package com.example.backend.exception;

/** The request is well-formed, but the game is not in a state where it can be done: not your turn,
 * already in progress, full, and similar. Maps to BAD_REQUEST so the client sees the real reason
 * instead of a generic "Internal server error". */
public class GameStateException extends RuntimeException {
    public GameStateException(String message) {
        super(message);
    }
}
