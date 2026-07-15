package com.asdru.asdrulet5.party.exception;

public class NotYetLootedException extends RuntimeException {

    public NotYetLootedException(String code, String userId) {
        super("User " + userId + " hasn't looted the chest yet in party " + code);
    }
}
