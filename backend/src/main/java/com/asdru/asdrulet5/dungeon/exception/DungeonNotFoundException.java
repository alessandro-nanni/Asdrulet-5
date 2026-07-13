package com.asdru.asdrulet5.dungeon.exception;

public class DungeonNotFoundException extends RuntimeException {

    public DungeonNotFoundException(String code) {
        super("No dungeon found for party: " + code);
    }
}
