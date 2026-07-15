package com.asdru.asdrulet5.party.exception;

public class ItemNotInShopException extends RuntimeException {

    public ItemNotInShopException(String code, String itemId) {
        super("Party " + code + "'s shop isn't selling " + itemId + " right now");
    }
}
