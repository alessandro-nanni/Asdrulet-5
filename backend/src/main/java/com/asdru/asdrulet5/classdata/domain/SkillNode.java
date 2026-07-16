package com.asdru.asdrulet5.classdata.domain;

import java.util.Objects;

import static com.asdru.asdrulet5.classdata.domain.Preconditions.requireNonBlank;

/**
 * One entry in a class's {@link SkillTree} — either upgrades an existing
 * ability's numbers or unlocks a brand new one (see {@link SkillNodeEffect}),
 * gated by {@code manaCost} and requiring {@code parentId} (null for the
 * tree's root) already unlocked. Prerequisite/mana validation happens at
 * unlock time (see party.PartyService/Party), not here — this is pure data,
 * resolved into an effective ability list by {@link SkillTreeResolver}.
 */
public record SkillNode(
        String id,
        String name,
        String description,
        int manaCost,
        String parentId,
        SkillNodeEffect effect
) {
    public SkillNode {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(description, "description");
        Objects.requireNonNull(effect, "effect");
        if (manaCost < 0) {
            throw new IllegalArgumentException("manaCost must not be negative");
        }
    }
}
