package com.asdru.asdrulet5.party.exception;

public class SkillAlreadyUnlockedException extends RuntimeException {

    public SkillAlreadyUnlockedException(String code, String userId, String nodeId) {
        super(userId + " in party " + code + " has already unlocked " + nodeId);
    }
}
