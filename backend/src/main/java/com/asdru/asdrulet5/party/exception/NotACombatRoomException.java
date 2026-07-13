package com.asdru.asdrulet5.party.exception;

import com.asdru.asdrulet5.dungeon.domain.RoomType;

public class NotACombatRoomException extends RuntimeException {

    public NotACombatRoomException(String code, RoomType roomType) {
        super("Current room in party " + code + " is " + roomType + ", not a combat room");
    }
}
