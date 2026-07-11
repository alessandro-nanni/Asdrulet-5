package com.asdru.asdrulet5.party.exception;

public class NotPartyMemberException extends RuntimeException {

    public NotPartyMemberException(String code, String userId) {
        super("User " + userId + " is not a member of party " + code);
    }
}
