package com.asdru.asdrulet5.party.exception;

public class InvalidStorageIndexException extends RuntimeException {

    public InvalidStorageIndexException(String code, int storageIndex) {
        super("Party " + code + " has no storage cell at index " + storageIndex);
    }
}
