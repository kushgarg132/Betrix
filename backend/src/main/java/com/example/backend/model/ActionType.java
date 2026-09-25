package com.example.backend.model;

/** A betting action a player takes on their own turn. Leaving, sitting out and sitting in are not
 * turn actions — they have their own mutations (leaveGame/sitOut/sitIn) and can happen any time. */
public enum ActionType {
    BET, CHECK, FOLD
}
