package com.asdru.asdrulet5.party.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Items currently for sale in the MERCHANT room the party has entered —
 * rolled fresh each time one is entered (see {@link #roll}) and consumed as
 * members buy from it. Doesn't know about coins or prices itself — Party's
 * own {@code buyFromShop} owns that check, since affording a purchase spans
 * this stock, the party's shared coin balance, and shared storage.
 *
 * <p>No locking of its own — see {@link RoomTurnProgress}'s own note on why.
 */
class ShopStock {

    private List<String> itemIds = List.of();

    void roll(List<String> newItemIds) {
        itemIds = List.copyOf(newItemIds);
    }

    List<String> list() {
        return itemIds;
    }

    boolean contains(String itemId) {
        return itemIds.contains(itemId);
    }

    void remove(String itemId) {
        List<String> updated = new ArrayList<>(itemIds);
        updated.remove(itemId);
        itemIds = updated;
    }
}
