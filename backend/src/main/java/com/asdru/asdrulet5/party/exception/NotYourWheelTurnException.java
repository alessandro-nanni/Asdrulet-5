package com.asdru.asdrulet5.party.exception;

public class NotYourWheelTurnException extends RuntimeException {

    public NotYourWheelTurnException(String code, String userId) {
        super("It is not " + userId + "'s turn to spin the wheel in party " + code);
    }
}
