package com.asdru.asdrulet5.party.exception;

public class PartyFullException extends RuntimeException {

    public PartyFullException(String code, int maxMembers) {
        super("Party " + code + " already has the maximum of " + maxMembers + " members");
    }
}
