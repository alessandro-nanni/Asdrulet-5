package com.asdru.asdrulet5.combat.exception;

public class UnknownAbilityException extends RuntimeException {

    public UnknownAbilityException(String actorId, String abilityId) {
        super(actorId + " has no ability " + abilityId);
    }
}
