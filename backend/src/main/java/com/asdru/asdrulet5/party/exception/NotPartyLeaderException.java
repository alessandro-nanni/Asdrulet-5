package com.asdru.asdrulet5.party.exception;

public class NotPartyLeaderException extends RuntimeException {

    public NotPartyLeaderException(String code, String userId) {
        super("User " + userId + " is not the leader of party " + code);
    }
}
