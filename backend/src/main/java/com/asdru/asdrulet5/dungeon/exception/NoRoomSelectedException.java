package com.asdru.asdrulet5.dungeon.exception;

public class NoRoomSelectedException extends RuntimeException {

    public NoRoomSelectedException(String code) {
        super("No next room has been selected to enter in party " + code);
    }
}
