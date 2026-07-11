package com.asdru.asdrulet5.party.exception;

public class NotAFakeMemberException extends RuntimeException {

    public NotAFakeMemberException(String code, String userId) {
        super("User " + userId + " in party " + code + " is not a dev-created fake member");
    }
}
