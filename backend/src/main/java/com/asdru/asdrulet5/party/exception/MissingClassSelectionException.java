package com.asdru.asdrulet5.party.exception;

public class MissingClassSelectionException extends RuntimeException {

    public MissingClassSelectionException(String code) {
        super("Every member of party " + code + " must select a class before starting");
    }
}
