package com.asdru.asdrulet5.classdata.exception;

import com.asdru.asdrulet5.party.domain.CharacterClass;

public class UnknownSkillNodeException extends RuntimeException {

    public UnknownSkillNodeException(CharacterClass characterClass, String nodeId) {
        super("No skill node '" + nodeId + "' in " + characterClass + "'s skill tree");
    }
}
