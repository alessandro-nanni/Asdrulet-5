package com.asdru.asdrulet5.party.exception;

public class NotInLootRoomException extends RuntimeException {

    public NotInLootRoomException(String code) {
        super("Party " + code + " has no chest to loot right now");
    }
}
