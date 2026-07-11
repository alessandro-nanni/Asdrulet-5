package com.asdru.asdrulet5.party.exception;

public class InvalidTurnOrderException extends RuntimeException {

    public InvalidTurnOrderException(String code) {
        super("Turn order must contain every current party member exactly once for party " + code);
    }
}
