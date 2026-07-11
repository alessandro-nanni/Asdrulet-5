package com.asdru.asdrulet5.classdata.domain;

import com.asdru.asdrulet5.party.domain.CharacterClass;

import java.util.List;
import java.util.Objects;

import static com.asdru.asdrulet5.classdata.domain.Preconditions.requireNonBlank;

public record ClassDefinition(
        CharacterClass characterClass,
        String displayName,
        String flavorText,
        Stats stats,
        List<Ability> abilities
) {
    public ClassDefinition {
        Objects.requireNonNull(characterClass, "characterClass");
        requireNonBlank(displayName, "displayName");
        requireNonBlank(flavorText, "flavorText");
        Objects.requireNonNull(stats, "stats");
        abilities = List.copyOf(abilities);

        long ultimateCount = abilities.stream().filter(a -> a instanceof UltimateAbility).count();
        long basicCount = abilities.size() - ultimateCount;
        if (ultimateCount != 1) {
            throw new IllegalArgumentException(
                    characterClass + " must define exactly one UltimateAbility, found " + ultimateCount);
        }
        if (basicCount < 2) {
            throw new IllegalArgumentException(
                    characterClass + " must define at least two BasicAbility entries, found " + basicCount);
        }
    }
}
