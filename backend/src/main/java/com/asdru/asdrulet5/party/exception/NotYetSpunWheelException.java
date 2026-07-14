package com.asdru.asdrulet5.party.exception;

public class NotYetSpunWheelException extends RuntimeException {

    public NotYetSpunWheelException(String code, String userId) {
        super("User " + userId + " hasn't spun the wheel yet in party " + code);
    }
}
