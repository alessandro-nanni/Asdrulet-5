package com.asdru.asdrulet5.party.exception;

public class SkillPrerequisiteNotMetException extends RuntimeException {

    public SkillPrerequisiteNotMetException(String code, String userId, String nodeId, String parentNodeId) {
        super(userId + " in party " + code + " must unlock " + parentNodeId + " before " + nodeId);
    }
}
