package com.asdru.asdrulet5.party.exception;

public class NotInMerchantRoomException extends RuntimeException {

    public NotInMerchantRoomException(String code) {
        super("Party " + code + " has no shop open right now");
    }
}
