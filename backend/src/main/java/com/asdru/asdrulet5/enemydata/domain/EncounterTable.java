package com.asdru.asdrulet5.enemydata.domain;

import java.util.List;
import java.util.Random;

/**
 * A named, weighted pool of enemies plus how many spawn in a single roll
 * (see {@link EncounterSize}) — e.g. a regular floor-1 fight rolls a random
 * 2-3, while a boss encounter is its own fixed-1 table over a single entry.
 */
public record EncounterTable(List<EnemyPoolEntry> entries, EncounterSize size) {

    public EncounterTable {
        entries = List.copyOf(entries);
    }

    /**
     * Rolls this table once: draws {@link #size}'s count of enemies — scaled
     * for partySize (see {@link EncounterSize#scaledForPartySize}), so a
     * bigger party faces proportionally more of them — with replacement
     * (see {@link EnemyPool}), and returns their enemy ids.
     */
    public List<String> roll(Random random, int partySize) {
        int count = size.scaledForPartySize(partySize).roll(random);
        return EnemyPool.pickRandom(entries, count, random).stream().map(EnemyPoolEntry::enemyId).toList();
    }
}
