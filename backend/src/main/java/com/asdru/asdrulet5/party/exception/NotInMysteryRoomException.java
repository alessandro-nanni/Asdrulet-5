package com.asdru.asdrulet5.party.exception;

public class NotInMysteryRoomException extends RuntimeException {

    public NotInMysteryRoomException(String code) {
        super("Party " + code + " has no mystery wheel to spin right now");
    }
}
