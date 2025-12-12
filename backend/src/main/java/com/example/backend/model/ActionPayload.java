package com.example.backend.model;

import lombok.Data;

@Data
public class ActionPayload {
    private String playerId;
    private ActionType actionType;
    private double amount;

    public enum ActionType {
        BET("BET"), CHECK("CHECK"), FOLD("FOLD"), LEAVE("LEAVE"), SIT_OUT("SIT_OUT"), SIT_IN("SIT_IN");

        public final String value;

        ActionType(String check) {
            this.value = check;
        }
    }
}
