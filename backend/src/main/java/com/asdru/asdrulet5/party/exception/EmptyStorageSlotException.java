package com.asdru.asdrulet5.party.exception;

public class EmptyStorageSlotException extends RuntimeException {

    public EmptyStorageSlotException(String code, int storageIndex) {
        super("Party " + code + " has no item in storage cell " + storageIndex);
    }
}
