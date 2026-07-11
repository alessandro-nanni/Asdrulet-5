package com.asdru.asdrulet5.party.exception;

public class PartyNotFoundException extends RuntimeException {

    public PartyNotFoundException(String code) {
        super("Party not found: " + code);
    }
}
