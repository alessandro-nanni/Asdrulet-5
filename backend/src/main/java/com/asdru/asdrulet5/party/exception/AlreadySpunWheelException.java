package com.asdru.asdrulet5.party.exception;

public class AlreadySpunWheelException extends RuntimeException {

    public AlreadySpunWheelException(String code, String userId) {
        super("User " + userId + " already spun the wheel in party " + code + " this room");
    }
}
