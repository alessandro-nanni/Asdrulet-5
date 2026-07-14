package com.asdru.asdrulet5.dungeon.exception;

public class RoomAlreadyEnteredException extends RuntimeException {

    public RoomAlreadyEnteredException(String code) {
        super("A room is already entered and not yet cleared in party " + code);
    }
}
