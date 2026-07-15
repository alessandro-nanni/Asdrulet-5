package com.asdru.asdrulet5.party.exception;

public class AlreadyLootedException extends RuntimeException {

    public AlreadyLootedException(String code, String userId) {
        super("User " + userId + " already looted the chest in party " + code + " this room");
    }
}
