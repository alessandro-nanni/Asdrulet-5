package com.asdru.asdrulet5.enemydata.domain;

import java.util.Random;

/**
 * How many enemies a single {@link EncounterTable} roll spawns — either
 * fixed (a boss's exactly 1) or a random range (a regular floor-1 fight's
 * 2-3, before party-size scaling — see {@link #scaledForPartySize}).
 * {@code min == max} represents a fixed amount.
 */
public record EncounterSize(int min, int max) {

    /**
     * The party size every {@link EncounterTable}/{@code EnemyDefinitionRegistry}
     * stats scaling formula treats as "unscaled" — a solo player faces
     * exactly this size's own min/max, a boss at its own base stats. Shared
     * here (rather than duplicated as a magic number in {@code CombatService}'s
     * own stats-scaling formula) so both stay in lockstep.
     */
    public static final int BASELINE_PARTY_SIZE = 1;

    public EncounterSize {
        if (min <= 0) {
            throw new IllegalArgumentException("min must be positive");
        }
        if (max < min) {
            throw new IllegalArgumentException("max must not be less than min");
        }
    }

    public static EncounterSize fixed(int amount) {
        return new EncounterSize(amount, amount);
    }

    /**
     * Scales this size for a party of partySize — {@link #BASELINE_PARTY_SIZE}
     * (solo) is unscaled; each additional player adds one more enemy to both
     * ends of the range, so a full party faces a proportionally bigger fight
     * instead of the same handful of enemies regardless of headcount. A
     * fixed size (boss encounters, {@code min == max}) scales the same way —
     * {@code CombatService} never actually calls this for a boss fight
     * (bosses stay at exactly 1, see its own stats-scaling instead), but
     * nothing here assumes otherwise.
     */
    public EncounterSize scaledForPartySize(int partySize) {
        int extraPlayers = Math.max(0, partySize - BASELINE_PARTY_SIZE);
        return new EncounterSize(min + extraPlayers, max + extraPlayers);
    }

    /**
     * Rolls a single amount within [min, max], inclusive.
     */
    public int roll(Random random) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }
}
