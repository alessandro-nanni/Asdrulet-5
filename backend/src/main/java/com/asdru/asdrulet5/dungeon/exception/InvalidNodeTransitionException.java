package com.asdru.asdrulet5.dungeon.exception;

public class InvalidNodeTransitionException extends RuntimeException {

    public InvalidNodeTransitionException(String code, String fromNodeId, String targetNodeId) {
        super("Node " + targetNodeId + " is not reachable from " + fromNodeId + " in party " + code);
    }
}
