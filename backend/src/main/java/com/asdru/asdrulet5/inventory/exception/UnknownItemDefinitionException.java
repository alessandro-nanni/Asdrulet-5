package com.asdru.asdrulet5.inventory.exception;

public class UnknownItemDefinitionException extends RuntimeException {

    public UnknownItemDefinitionException(String itemId) {
        super("No item definition registered for " + itemId);
    }
}
