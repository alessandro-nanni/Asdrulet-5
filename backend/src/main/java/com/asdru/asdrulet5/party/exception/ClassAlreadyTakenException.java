package com.asdru.asdrulet5.party.exception;

import com.asdru.asdrulet5.party.domain.CharacterClass;

public class ClassAlreadyTakenException extends RuntimeException {

    public ClassAlreadyTakenException(String code, CharacterClass characterClass) {
        super("Class " + characterClass + " is already taken in party " + code);
    }
}
