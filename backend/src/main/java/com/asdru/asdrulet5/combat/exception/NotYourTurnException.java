package com.asdru.asdrulet5.combat.exception;

public class NotYourTurnException extends RuntimeException {

    public NotYourTurnException(String actorId) {
        super("It is not " + actorId + "'s turn");
    }
}
