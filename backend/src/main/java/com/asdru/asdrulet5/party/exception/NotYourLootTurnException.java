package com.asdru.asdrulet5.party.exception;

public class NotYourLootTurnException extends RuntimeException {

    public NotYourLootTurnException(String code, String userId) {
        super("It is not " + userId + "'s turn to loot the chest in party " + code);
    }
}
