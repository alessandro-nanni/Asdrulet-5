package com.asdru.asdrulet5.party.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The party's shared item-storage grid — a fixed number of cells (see
 * {@link Party#STORAGE_SIZE}), rendered by the frontend as 3 columns x 4
 * rows. Cells are legitimately null (empty); item ids themselves aren't
 * validated here — that's ItemDefinitionRegistry's job, kept out of this
 * package the same way it's kept out of Party.
 *
 * <p>No locking of its own — see {@link RoomTurnProgress}'s own note on why.
 */
class SharedStorage {

    private final List<String> cells;

    SharedStorage(int size) {
        this.cells = new ArrayList<>(Collections.nCopies(size, null));
    }

    /**
     * Fills cells from the front — called once, right after creation, with whatever the catalog currently holds.
     */
    void seed(List<String> itemIds) {
        for (int i = 0; i < itemIds.size() && i < cells.size(); i++) {
            cells.set(i, itemIds.get(i));
        }
    }

    List<String> list() {
        // Not List.copyOf: cells are legitimately null (empty), and
        // List.copyOf/List.of both reject null elements.
        return Collections.unmodifiableList(new ArrayList<>(cells));
    }

    int size() {
        return cells.size();
    }

    String at(int index) {
        return cells.get(index);
    }

    void set(int index, String itemId) {
        cells.set(index, itemId);
    }

    /**
     * Drops itemId into the first empty cell; a no-op if the grid is already full.
     */
    void addFirstEmpty(String itemId) {
        int emptyIndex = cells.indexOf(null);
        if (emptyIndex >= 0) {
            cells.set(emptyIndex, itemId);
        }
    }
}
