package com.asdru.asdrulet5.combat.exception;

public class CombatNotInProgressException extends RuntimeException {

    public CombatNotInProgressException(String code) {
        super("Combat for party " + code + " is no longer in progress");
    }
}
