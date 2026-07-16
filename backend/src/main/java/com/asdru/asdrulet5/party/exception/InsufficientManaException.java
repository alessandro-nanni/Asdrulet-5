package com.asdru.asdrulet5.party.exception;

public class InsufficientManaException extends RuntimeException {

    public InsufficientManaException(String code, String userId, int manaCost, int mana) {
        super(userId + " in party " + code + " has " + mana + " mana, but this skill costs " + manaCost);
    }
}
