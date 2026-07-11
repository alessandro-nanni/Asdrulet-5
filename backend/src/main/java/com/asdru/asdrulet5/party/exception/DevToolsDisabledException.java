package com.asdru.asdrulet5.party.exception;

public class DevToolsDisabledException extends RuntimeException {

    public DevToolsDisabledException() {
        super("Dev tools are disabled");
    }
}
