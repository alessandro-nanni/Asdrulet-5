package com.asdru.asdrulet5.party.exception;

public class InsufficientCoinsException extends RuntimeException {

    public InsufficientCoinsException(String code, int price, int coins) {
        super("Party " + code + " has " + coins + " coins, but this costs " + price);
    }
}
