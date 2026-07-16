package com.asdru.asdrulet5.party.exception;

public class ItemNotConsumableException extends RuntimeException {

    public ItemNotConsumableException(String code, String itemId) {
        super("Party " + code + "'s item " + itemId + " cannot be consumed");
    }
}
