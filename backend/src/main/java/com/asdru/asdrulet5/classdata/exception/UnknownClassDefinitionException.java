package com.asdru.asdrulet5.classdata.exception;

import com.asdru.asdrulet5.party.domain.CharacterClass;

public class UnknownClassDefinitionException extends RuntimeException {

    public UnknownClassDefinitionException(CharacterClass characterClass) {
        super("No class definition registered for " + characterClass);
    }
}
