package com.asdru.asdrulet5.combat.exception;

public class CombatNotFoundException extends RuntimeException {

    public CombatNotFoundException(String code) {
        super("No combat in progress for party: " + code);
    }
}
